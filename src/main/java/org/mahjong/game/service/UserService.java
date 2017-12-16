package org.mahjong.game.service;

import org.mahjong.game.config.SystemWebSocketHandler;
import org.mahjong.game.domain.Room;
import org.mahjong.game.domain.User;
import org.mahjong.game.dtos.JsonResult;
import org.mahjong.game.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static Logger log = LoggerFactory.getLogger(UserService.class);

    @Inject
    private UserRepository userRepository;

    @Inject
    private RoomService roomService;

    public void save(User user) {
        userRepository.save(user);
    }

    public User getUserFromSession(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        String username = Optional.ofNullable(session.getAttributes().get("username")).orElse("").toString();
        if (username.equals("")) {
            result.setMessage("用户未登录");
            session.sendMessage(new TextMessage(result.toString()));
            return null;
        }
        Optional<User> _user = userRepository.findOneByUserName(username);
        if (!_user.isPresent()) {
            result.setMessage("用户不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return null;
        }
        User user = _user.get();
        return user;
    }

    public User getUserFromUsername(String username) {
        Optional<User> _user = userRepository.findOneByUserName(username);
        if (!_user.isPresent()) {
            return null;
        }
        User user = _user.get();
        return user;
    }

    /**
     * 游戏结束后要重置user的部分信息
     */
    public void resetUser(User user) {
        user.setReady(false);
//        user.setRoom(null);//仍然停留在原房间,index也不改变
        user.setThrownTiles(null);
        user.setOwnTiles(null);
        save(user);
    }

    @Transactional
    public void login(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult jsonResult = new JsonResult();
        if (payloadArray.length != 3) {
            jsonResult.setMessage("登录消息格式错误");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        String username = payloadArray[1];
        String password = payloadArray[2];
        Optional<User> _user = userRepository.findOneByUserName(username.trim());
        if (!_user.isPresent() || !_user.get().getPassword().equals(DigestUtils.md5DigestAsHex(password.getBytes()))) {
            jsonResult.setMessage("用户名或密码错误");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        jsonResult.setStatus(true);
        jsonResult.setMessage("登陆成功");
        session.sendMessage(new TextMessage(jsonResult.toString()));

        session.getAttributes().put("username", username);
        SystemWebSocketHandler.sessions.add(session);
        SystemWebSocketHandler.sessionsMap.put(username, session);//key是userName
        log.info("用户{}登陆成功, 分配了session {}", username, session.getId());

    }

    @Transactional
    public void register(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult jsonResult = new JsonResult();
        if (payloadArray.length != 3) {
            jsonResult.setMessage("注册消息格式错误");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        String username = payloadArray[1];
        String password = payloadArray[2];
        Optional<User> _user = userRepository.findOneByUserName(username.trim());
        if (_user.isPresent()) {
            jsonResult.setMessage("用户名重复");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        User user = new User();
        user.setUserName(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        userRepository.save(user);

        jsonResult.setStatus(true);
        jsonResult.setMessage("注册成功");
        session.sendMessage(new TextMessage(jsonResult.toString()));
        log.info("用户{}注册成功, session {}", username, session.getId());
    }


    /**
     * 发送准备请求
     */
    public void setReady(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        User user = getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入随机房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            log.error("玩家{}不在房间中，无法准备", user.getUserName());
            result.setMessage("玩家已经在房间中，无法接受邀请");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Room room = user.getRoom();
        if (RoomService.roomMap.containsKey(room.getId())) {//不在房间里
            result.setMessage("房间不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        user.setReady(true);
        userRepository.save(user);

        //广播
        //给房间里所有人发消息，更新画面
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
            roomService.broadcastRoomPlayers(socketSession);
        }
    }
}

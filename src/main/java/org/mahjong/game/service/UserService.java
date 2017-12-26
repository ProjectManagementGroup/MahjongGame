package org.mahjong.game.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static Logger log = LoggerFactory.getLogger(UserService.class);

    private Map<String, User> allUsers = Maps.newLinkedHashMap();

    @Inject
    private UserRepository userRepository;

    @Inject
    private RoomService roomService;

    public void save(User user) {
        userRepository.save(user);
        allUsers.put(user.getUsername(), user);
    }

    public User getUserFromSession(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        String username = Optional.ofNullable(session.getAttributes().get("username")).orElse("").toString();
        if (username.equals("")) {
            result.setMessage("用户未登录");
            session.sendMessage(new TextMessage(result.toString()));
            return null;
        }
        if (!allUsers.containsKey(username)) {
            result.setMessage("用户不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return null;
        }
        User user = allUsers.get(username);
        return user;
    }

    public User getUserFromUsername(String username) {
        if (!allUsers.containsKey(username)) {
            return null;
        }
        User user = allUsers.get(username);
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
        user.setIndex(0);
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
        Optional<User> _user = userRepository.findOneByUsername(username.trim());
        if (!_user.isPresent() || !_user.get().getPassword().equals(DigestUtils.md5DigestAsHex(password.getBytes()))) {
            jsonResult.setMessage("用户名或密码错误");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        jsonResult.setStatus(true);
        jsonResult.setMessage("login success");
        session.sendMessage(new TextMessage(jsonResult.toString()));

        session.getAttributes().put("username", username);
        SystemWebSocketHandler.sessions.add(session);
        SystemWebSocketHandler.sessionsMap.put(username, session);//key是userName
        log.info("用户{}登陆成功, 分配了session {}", username, session.getId());

        //加入list
        User user = _user.get();
        allUsers.put(user.getUsername(), user);

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
        Optional<User> _user = userRepository.findOneByUsername(username.trim());
        if (_user.isPresent()) {
            jsonResult.setMessage("用户名重复");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        userRepository.save(user);

        jsonResult.setStatus(true);
        jsonResult.setMessage("register success");
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
            log.error("玩家{}不在房间中，无法准备", user.getUsername());
            result.setMessage("玩家不在房间中，无法接受邀请");
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
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUsername())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUsername());
            roomService.broadcastRoomPlayers(socketSession);
        }
    }
}

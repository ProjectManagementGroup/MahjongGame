package org.mahjong.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.mahjong.game.Constants;
import org.mahjong.game.config.SystemWebSocketHandler;
import org.mahjong.game.domain.FriendRelation;
import org.mahjong.game.domain.Room;
import org.mahjong.game.domain.User;
import org.mahjong.game.dtos.JsonResult;
import org.mahjong.game.repository.FriendRelationRepository;
import org.mahjong.game.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
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


    @Inject
    private UserRepository userRepository;

    @Inject
    private RoomService roomService;

    @Inject
    private FriendRelationRepository friendRelationRepository;

    private static ObjectMapper objectMapper = new ObjectMapper();//用于网络发包

    @Transactional
    @Async
    public synchronized void save(User user) {
        userRepository.save(user);
        Constants.allUsers.put(user.getName(), user);
    }

    @Transactional
    @Async
    public synchronized User getUserFromSession(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        String username = Optional.ofNullable(session.getAttributes().get("username")).orElse("").toString();
        if (username.equals("")) {
            result.setMessage("用户未登录");
            session.sendMessage(new TextMessage(result.toString()));
            return null;
        }
        if (!Constants.allUsers.containsKey(username)) {
            result.setMessage("用户不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return null;
        }
        User user = Constants.allUsers.get(username);
        return user;
    }

    @Transactional
    @Async
    public synchronized User getUserFromUsername(String username) {
        if (!Constants.allUsers.containsKey(username)) {
            return null;
        }
        User user = Constants.allUsers.get(username);
        return user;
    }

    /**
     * 游戏结束后要重置user的部分信息
     */
    @Transactional
    @Async
    public synchronized void resetUser(User user) {
        user.setReady(false);
//        user.setRoom(null);//仍然停留在原房间,index也不改变
        user.setThrownTiles(Lists.newLinkedList());
        user.setOwnTiles(Lists.newLinkedList());
        user.setGameid(-1);
        save(user);
    }

    @Transactional
    @Async
    public synchronized void login(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult jsonResult = new JsonResult();
        if (payloadArray.length != 3) {
            jsonResult.setMessage("login");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        String username = payloadArray[1];
        String password = payloadArray[2];
        Optional<User> _user = userRepository.findOneByName(username.trim());
        if (!_user.isPresent() || !_user.get().getPassword().equals(DigestUtils.md5DigestAsHex(password.getBytes()))) {
            jsonResult.setMessage("login");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        jsonResult.setStatus(true);
        jsonResult.setMessage("login");

        //TODO:可能需要一个重复登录的验证
        if (SystemWebSocketHandler.sessionsMap.containsKey(username)) {
            jsonResult.setMessage("不允许重复登录！！");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        User user = _user.get();
        Constants.allUsers.put(user.getName(), user);

        Map<String, Object> map = Maps.newLinkedHashMap();
        map.put("name", username);
        map.put("point", user.getPoint());
        //把自己的好友列表也给出去
        List<String> friendList = friendRelationRepository.findAllByUseId1(user.getId());
        friendList.addAll(friendRelationRepository.findAllByUseId2(user.getId()));
        map.put("friendList", friendList);
        jsonResult.setObject(objectMapper.writeValueAsString(map));
        session.sendMessage(new TextMessage(jsonResult.toString()));

        session.getAttributes().put("username", username);
        SystemWebSocketHandler.sessions.add(session);
        SystemWebSocketHandler.sessionsMap.put(username, session);//key是userName
        log.info("用户{}登陆成功, 分配了session {}", username, session.getId());

    }

    /**
     * 用户请求加好友
     * 如果已经是好友，那么直接返回
     *
     * @param payloadArray
     * @param session
     * @throws Exception
     */
    @Transactional
    @Async
    public synchronized void sendFriendInvitation(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        if (payloadArray.length != 2) {
            result.setMessage("请求信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        User user = getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入申请好友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //就不判断是不是在房间里了

        //邀请好友的session
        User friend = getUserFromUsername(payloadArray[1]);
        if (friend == null) {
            result.setMessage("好友信息错误，无法加入申请好友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Long id = friendRelationRepository.findRelationByUser1AndUser2(user.getId(), friend.getId());
        if (id != null) {
            result.setMessage("二人已经是好友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //就不判断是不是在房间里了
        if (!SystemWebSocketHandler.sessionsMap.containsKey(payloadArray[1])) {
            log.error("好友{}不在线，无法加入申请好友", payloadArray[1]);
            result.setMessage("好友不在线，无法加入申请好友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        WebSocketSession friendSession = SystemWebSocketHandler.sessionsMap.get(payloadArray[1]);

        //给被邀请的人发消息
        //要把邀请人的信息和其他被邀请的人的消息都发送出去
        result.setStatus(true);
        result.setMessage("friendInvitation");
        //邀请者
        result.setObject(user.getName());
        friendSession.sendMessage(new TextMessage(result.toString()));

        //不给申请人发消息了

        log.info("用户{}申请好友，session {}，申请好友{}/{}", user.getName(), session.getId(), friend.getName(), friendSession.getId());

    }

    /**
     * 接受好友请求
     */
    @Transactional
    @Async
    public synchronized void friendAccept(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        if (payloadArray.length != 2) {
            result.setMessage("请求信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        User user = getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入申请好友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //就不判断是不是在房间里了

        //邀请好友的session
        User friend = getUserFromUsername(payloadArray[1]);
        if (friend == null) {
            result.setMessage("好友信息错误，无法加入申请好友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //就不判断是不是在房间里了
        if (!SystemWebSocketHandler.sessionsMap.containsKey(payloadArray[1])) {
            log.error("好友{}不在线，无法加入申请好友", payloadArray[1]);
            result.setMessage("好友不在线，无法加入申请好友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        WebSocketSession friendSession = SystemWebSocketHandler.sessionsMap.get(payloadArray[1]);

        //在数据库里面加一条数据
        FriendRelation relation = new FriendRelation();
        relation.setUser1(user);
        relation.setUser2(friend);
        friendRelationRepository.save(relation);

        //告诉被申请者成功,推送好友列表
        result.setStatus(true);
        result.setMessage("friendAccept");
        //被申请者
        List<String> friendList = friendRelationRepository.findAllByUseId1(friend.getId());
        friendList.addAll(friendRelationRepository.findAllByUseId2(friend.getId()));
        result.setObject(objectMapper.writeValueAsString(friendList));
        friendSession.sendMessage(new TextMessage(result.toString()));

        //告诉申请者成功,推送好友列表
        result.setStatus(true);
        result.setMessage("friendAccept");
        //申请者
        friendList = friendRelationRepository.findAllByUseId1(user.getId());
        friendList.addAll(friendRelationRepository.findAllByUseId2(user.getId()));
        result.setObject(objectMapper.writeValueAsString(friendList));
        session.sendMessage(new TextMessage(result.toString()));

        log.info("用户{}/{} 接受用户 {}/{} 的好友申请", user.getName(), session.getId(), friend.getName(), friendSession.getId());

    }

    /**
     * 请求好友列表
     *
     * @param session
     * @throws Exception
     */
    @Transactional
    @Async
    public synchronized void askFriendList(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        User user = getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法请求好友列表");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //从数据库里查出来
        List<String> friendList = friendRelationRepository.findAllByUseId1(user.getId());
        friendList.addAll(friendRelationRepository.findAllByUseId2(user.getId()));

        //下面判断每个好友是否在线
//        for (User u : friendList) {
//            Map<String, Object> map = Maps.newLinkedHashMap();
//            map.put("name", u.getName());
//            map.put("online", SystemWebSocketHandler.sessionsMap.containsKey(u.getName()));
//            list.add(map);
//        }
        result.setStatus(true);
        result.setMessage("friendList");
        result.setObject(objectMapper.writeValueAsString(friendList));

        session.sendMessage(new TextMessage(result.toString()));
    }

    @Transactional
    @Async
    public synchronized void register(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult jsonResult = new JsonResult();
        if (payloadArray.length != 3) {
            jsonResult.setMessage("register");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        String username = payloadArray[1];
        String password = payloadArray[2];
        Optional<User> _user = userRepository.findOneByName(username.trim());
        if (_user.isPresent()) {
            jsonResult.setMessage("register");
            session.sendMessage(new TextMessage(jsonResult.toString()));
            return;
        }
        User user = new User();
        user.setName(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        save(user);

        jsonResult.setStatus(true);
        jsonResult.setMessage("register");
        session.sendMessage(new TextMessage(jsonResult.toString()));
        log.info("用户{}注册成功, session {}", username, session.getId());
    }


    /**
     * 发送准备请求
     */
    @Transactional
    @Async
    public synchronized void setReady(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        User user = getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入随机房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            log.error("玩家{}不在房间中，无法准备", user.getName());
            result.setMessage("玩家不在房间中，无法接受邀请");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Room room = user.getRoom();
        if (!Constants.roomMap.containsKey(room.getId())) {//不在房间里
            result.setMessage("房间不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        user.setReady(true);
        save(user);
        log.info("用户{}, session {}, 房间id{}，准备了", user.getName(), session.getId(), room.getId());
        //广播
        //给房间里所有人发消息，更新画面
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            roomService.broadcastRoomPlayers(socketSession);
        }
    }
}

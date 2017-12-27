package org.mahjong.game.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.mahjong.game.Constants;
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
public class RoomService {

    private static Logger log = LoggerFactory.getLogger(RoomService.class);

    /**
     * 随机组队且人数未满的房间，不排序，每次只寻找第一个
     * 如果人数满了，那么移动到playingRoomList集合里面
     */
    public static Map<Long, Room> roomMap = Maps.newLinkedHashMap();

    private static ObjectMapper objectMapper = new ObjectMapper();//用于网络发包


    @Inject
    private UserService userService;

    @Inject
    private UserRepository userRepository;


    /**
     * 创建一个房间
     *
     * @return
     */
    private synchronized Room createRoom() {
        Room room = new Room();
        Long id = Long.parseLong(DateTime.now().toString("yyyyMMddHHmmss"));//时间戳就是id，一定是唯一的
        room.setId(id);
        room.setFriendly(false);
        room.setIndex(0);
        room.setPlaying(false);
        List<Constants.MahjongTile> list = Lists.newLinkedList(Constants.getMahjongTiles());
        room.setMahjongTiles(list);
        log.info("创建房间{}", room.getId());
        return room;
    }

    /**
     * 游戏结束后要重置room的部分信息
     */
    public Room resetRoom(Room room) {
        List<Constants.MahjongTile> list = Lists.newLinkedList(Constants.getMahjongTiles());
        room.setMahjongTiles(list);
        room.setPlaying(false);
        room.setIndex(0);
        room.setLastTile(null);
        room.setTimerStart(null);
        room.setRequests(Lists.newLinkedList());
        log.info("重置房间{},并将房间内玩家设置为未准备状态", room.getId());
        return room;
    }

    /**
     * 由于加入房间之后要显示所有人，所以需要请求
     * 同时回复是否开始游戏的消息
     *
     * @param session
     * @throws Exception
     */
    public void broadcastRoomPlayers(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入随机房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            log.error("玩家{}不在房间中", user.getName());
            result.setMessage("玩家不在房间中");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (!roomMap.containsKey(user.getRoom().getId())) {
            result.setMessage("房间不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }

        result.setStatus(true);
        result.setMessage("room information");

        Map<String, Object> resultMap = Maps.newLinkedHashMap();

        //把非自己的所有玩家的可见信息全部广播
        List<Object> list = Lists.newLinkedList();
        for (User u : user.getRoom().getPlayers()) {
            if (u.getId() == user.getId()) {
                continue;
            }
            Map<String, Object> map = Maps.newLinkedHashMap();
            map.put("name", u.getName());
            map.put("point", u.getPoint());
            map.put("ready", u.isReady());
            map.put("index", u.getGameid());
//            map.put("thrownTiles", u.getJsonThrownTileLists());
            list.add(map);
        }
        resultMap.put("others", list);

        //加入自己的所有信息
        Map<String, Object> meMap = Maps.newLinkedHashMap();
        meMap.put("name", user.getName());
        meMap.put("point", user.getPoint());
        meMap.put("ready", user.isReady());
        meMap.put("index", user.getGameid());
//        meMap.put("ownTiles", user.getJsonOwnTileLists());
//        meMap.put("thrownTiles", user.getJsonThrownTileLists());

        resultMap.put("me", meMap);
        //房间内剩余牌量
//        resultMap.put("restTiles", user.getRoom().getMahjongTiles().size());
        result.setObject(objectMapper.writeValueAsString(resultMap));
        session.sendMessage(new TextMessage(result.toString()));

    }

    /**
     * 有个玩家想要随机加入房间游戏
     * 从人数最多的房间开始，如果没有空房间，那么自己创建一个房间，只有自己一个人
     * 使用synchronized是担心时间戳相同
     * 返回房间的id
     * 出现错误那么返回-1
     */
    public synchronized void joinRandomRoom(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入随机房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() != null) {
            result.setMessage("玩家已经在房间内，无法同时加入两个房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }

        //找到第一个*人数未满的非好友*房间并加入
        Room room = null;
        for (Map.Entry<Long, Room> entry : roomMap.entrySet()) {
            if (entry.getValue().getPlayers().size() < 4 && !entry.getValue().isFriendly()) {
                room = entry.getValue();
                break;
            }
        }
        if (room == null) {
            //如果没有少人的房间，那么新创建一个
            room = createRoom();
            room.getPlayers().add(user);
            user.setRoom(room);
            user.setGameid(0);//自己就是第一个
            userService.save(user);
            roomMap.put(room.getId(), room);
            log.info("玩家{}创建随机房间{}", user.getName(), room.getId());
            result.setStatus(true);
            result.setMessage("join random success");
            result.setObject(room.getId() + "");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }

        room.getPlayers().add(user);
        user.setRoom(room);
        user.setGameid(room.getPlayers().size());
        userService.save(user);

        result.setStatus(true);
        result.setMessage("join random success");
        result.setObject(room.getId() + "");
        session.sendMessage(new TextMessage(result.toString()));

        //给房间里所有人发消息包括自己，更新画面
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            broadcastRoomPlayers(socketSession);
        }
    }


    /**
     * 有个玩家受邀请加入好友房间游戏
     */
    public synchronized void joinFriendRoom(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        if (payloadArray.length != 2) {
            result.setMessage("请求信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入随机房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() != null) {
            log.error("玩家{}已经在房间中，无法接受邀请", user.getName());
            result.setMessage("玩家已经在房间中，无法接受邀请");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (!roomMap.containsKey(payloadArray[1])) {
            log.error("房间{}不存在", payloadArray[1]);
            result.setMessage("房间不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Room room = roomMap.get(payloadArray[1]);
        if (!room.isFriendly()) {
            result.setMessage("不是好友房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (room.getPlayers().size() == 4) {
            result.setMessage("好友房间人数已满4人, 加入好友房间失败");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        room.getPlayers().add(user);
        user.setRoom(room);
        user.setGameid(room.getPlayers().size());
        userService.save(user);

//        result.setStatus(true);
//        result.setMessage("accept success");
//        result.setObject(payloadArray[1]);
//        session.sendMessage(new TextMessage(result.toString()));

        //给房间里所有人发消息，更新画面
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            broadcastRoomPlayers(socketSession);
        }
    }

    /**
     * 有个玩家想要邀请好友
     * 首先自己要在房间里
     * 使用synchronized是担心时间戳相同
     * 返回发送请求是否
     */

    public synchronized void sendInvitationAndCreateRoom(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        if (payloadArray.length != 4) {
            result.setMessage("请求信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入好友房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() != null) {
            log.error("玩家{}已经在房间中，无法邀请队友", user.getName());
            result.setMessage("玩家已经在房间中，无法邀请队友");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //邀请好友的session
        List<WebSocketSession> sessions = Lists.newArrayListWithCapacity(3);
        List<User> friends = Lists.newArrayListWithCapacity(3);
        for (int i = 1; i < payloadArray.length; i++) {
            User friend1 = userService.getUserFromUsername(payloadArray[i]);
            if (friend1 == null) {
                result.setMessage("好友" + i + "信息错误，无法加入好友房间");
                session.sendMessage(new TextMessage(result.toString()));
                return;
            }
            if (friend1.getRoom() != null) {
                log.error("好友{}已经在房间中，无法邀请队友", payloadArray[i]);
                result.setMessage("好友" + i + "已经在房间中，无法邀请队友");
                session.sendMessage(new TextMessage(result.toString()));
                return;
            }
            if (!SystemWebSocketHandler.sessionsMap.containsKey(payloadArray[i])) {
                log.error("好友{}不在线，无法邀请队友", payloadArray[i]);
                result.setMessage("好友" + i + "不在线，无法邀请队友");
                session.sendMessage(new TextMessage(result.toString()));
                return;
            }
            WebSocketSession session1 = SystemWebSocketHandler.sessionsMap.get(payloadArray[i]);
            sessions.add(session1);
            friends.add(friend1);
        }

        Room room = createRoom();
        room.setFriendly(true);
        room.getPlayers().addAll(friends);

        //给被邀请的人发消息
        //要把邀请人的信息和其他被邀请的人的消息都发送出去
        result.setStatus(true);
        result.setMessage("invitation");

        Map<String, Object> map = Maps.newLinkedHashMap();
        map.put("banker", user.getName());
        for (int i = 0; i < friends.size(); i++) {
            map.put("name" + i, friends.get(i).getName());
        }
        map.put("room", room.getId());
        result.setObject(objectMapper.writeValueAsString(map));

        for (WebSocketSession s : sessions) {
            if (!s.isOpen()) {//仍要判断是否对方还在线
                log.error("好友{}不在线，无法邀请队友", s.getAttributes().get("username"));
                result.setMessage("好友不在线，无法邀请队友");
                result.setStatus(false);
                session.sendMessage(new TextMessage(result.toString()));
                return;
            }
            s.sendMessage(new TextMessage(result.toString()));
        }

        //给发送邀请的人发消息
        result.setMessage("invite success");
        result.setObject(null);
        session.sendMessage(new TextMessage(result.toString()));

        roomMap.put(room.getId(), room);
    }

    public void exit(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            log.error("玩家{}不在房间中，无法退出房间", user.getName());
            result.setMessage("玩家不在房间中，无法退出房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom().isPlaying()) {
            log.error("玩家{}正在游戏不能退出", user.getName());
            result.setMessage("玩家正在游戏不能退出");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //一人退出房间解散
        result.setStatus(true);
        result.setMessage("exit room");
        result.setObject("玩家" + user.getName() + "退出游戏房间解散");

        //这个房间就不存在了
        roomMap.remove(user.getRoom().getId());
        for (User u : user.getRoom().getPlayers()) {
            u.setGameid(-1);
            u.setRoom(null);
            u.setOwnTiles(Lists.newLinkedList());
            u.setThrownTiles(Lists.newLinkedList());
            u.setReady(false);
            userService.save(u);
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }
    }

    public void adjustUserIndex(Room room, User winner) {
        room.getPlayers().remove(winner);
        room.getPlayers().add(0, winner);
        for (int i = 0; i < room.getPlayers().size(); i++) {
            room.getPlayers().get(i).setGameid(i);
            userService.save(room.getPlayers().get(i));
        }
    }

    public static void main(String args[]) throws Exception {
//        List<Object> list = Lists.newLinkedList();

//        Map<String, Object> infoMap = Maps.newLinkedHashMap();
//        infoMap.put("username", "123");
//        infoMap.put("point", 123);
//        infoMap.put("ready", true);
//        list.add(infoMap);
//
//        infoMap = Maps.newLinkedHashMap();
//        infoMap.put("username", "456");
//        infoMap.put("point", 455);
//        infoMap.put("ready", false);
//        list.add(infoMap);
//
//        Map<String, Object> map = Maps.newLinkedHashMap();
//        map.put("list", list);
//        map.put("start", true);
//
//        JsonResult result = new JsonResult();
//        result.setObject(objectMapper.writeValueAsString(infoMap));
//        result.setStatus(true);
//        result.setMessage("other out");
//        System.out.println(result.toString());

//        JsonResult result = new JsonResult();
//        result.setStatus(true);
//        result.setMessage("room information");
//
//        Map<String, Object> resultMap = Maps.newLinkedHashMap();
//
//        //把非自己的所有玩家的可见信息全部广播
//        List<Object> list = Lists.newLinkedList();
//
//        Map<String, Object> map = Maps.newLinkedHashMap();
//        map.put("username", "000");
//        map.put("point", 000);
//        map.put("ready", false);
//        map.put("index", 0);
//        List<Object> list1 = Lists.newLinkedList();
//        list1.add(Constants.MahjongTile.eastWind.getStruct());
//        list1.add(Constants.MahjongTile.fiveBamboo.getStruct());
//        list1.add(Constants.MahjongTile.white.getStruct());
//        map.put("thrownTiles", list1);
//        list.add(map);
//
//        map = Maps.newLinkedHashMap();
//        map.put("username", "111");
//        map.put("point", 111);
//        map.put("ready", false);
//        map.put("index", 1);
//        list1 = Lists.newLinkedList();
//        list1.add(Constants.MahjongTile.twoBamboo.getStruct());
//        list1.add(Constants.MahjongTile.twoDot.getStruct());
//        list1.add(Constants.MahjongTile.middle.getStruct());
//        map.put("thrownTiles", list1);
//        list.add(map);
//
//        map = Maps.newLinkedHashMap();
//        map.put("username", "222");
//        map.put("point", 222);
//        map.put("ready", false);
//        map.put("index", 2);
//        list1 = Lists.newLinkedList();
//        list.add(Constants.MahjongTile.fortune.getStruct());
//        list.add(Constants.MahjongTile.fiveMyriad.getStruct());
//        list.add(Constants.MahjongTile.westWind.getStruct());
//        map.put("thrownTiles", list1);
//        list.add(map);
//
//        resultMap.put("others", list);
//
//        //加入自己的所有信息
//        Map<String, Object> me = Maps.newLinkedHashMap();
//        me.put("username", "333");
//        me.put("point", 333);
//        me.put("ready", true);
//        me.put("index", 3);
//        list1 = Lists.newLinkedList();
//        list1.add(Constants.MahjongTile.eightDot.getStruct());
//        list1.add(Constants.MahjongTile.fourBamboo.getStruct());
//        list1.add(Constants.MahjongTile.sixBamboo.getStruct());
//        me.put("ownTiles", list1);
//        me.put("thrownTiles", list1);
//
//        resultMap.put("me", me);
//
//        //房间内剩余牌量
//        resultMap.put("restTiles", 52);
//        result.setObject(objectMapper.writeValueAsString(resultMap));
//        System.out.println(result.toString());

//        JsonResult result = new JsonResult();
//        result.setStatus(true);
//        result.setMessage("game start allocate");
//
//        //给第一个玩家发14张牌，其他玩家13张牌
//        Map<String, Object> map = Maps.newLinkedHashMap();
//        map.put("own-index", 0);
//
//        List<Object> list1 = Lists.newLinkedList();
//        list1.add(Constants.MahjongTile.eastWind.getStruct());
//        list1.add(Constants.MahjongTile.fiveBamboo.getStruct());
//        list1.add(Constants.MahjongTile.white.getStruct());
//        map.put("own-tiles", list1);
//
//        result.setObject(objectMapper.writeValueAsString(map));
//        System.out.println(result.toString());

//        result.setStatus(true);
//        //向其他玩家广播
//        result.setMessage("bump success");
//        Map<String, Object> map = Maps.newLinkedHashMap();
//        map.put("username", "zyw");
//        map.put("index", 2);
//        map.put("type", "bump");
//        map.put("tile", Constants.MahjongTile.sixBamboo.getStruct());
//        result.setObject(objectMapper.writeValueAsString(map));
//        System.out.println(result);

        //广播胜利消息
//        JsonResult result = new JsonResult();
//        result.setStatus(true);
//        result.setMessage("game tie");
//
//        Map<String, Object> resultMap = Maps.newLinkedHashMap();
//
//        //加入胜利玩家信息
//        List<Object> list = Lists.newLinkedList();
//
//        Map<String, Object> map = Maps.newLinkedHashMap();
//        map.put("username", "111");
//        List<Object> list1 = Lists.newLinkedList();
//        list1.add(Constants.MahjongTile.eastWind.getStruct());
//        list1.add(Constants.MahjongTile.fiveBamboo.getStruct());
//        list1.add(Constants.MahjongTile.white.getStruct());
//        map.put("ownTiles", list1);
//        map.put("thrownTiles", list1);
//        map.put("index", 1);
//        list.add(map);
//
//        map = Maps.newLinkedHashMap();
//        map.put("username", "222");
//        list1 = Lists.newLinkedList();
//        list1.add(Constants.MahjongTile.eastWind.getStruct());
//        list1.add(Constants.MahjongTile.fiveBamboo.getStruct());
//        list1.add(Constants.MahjongTile.white.getStruct());
//        map.put("ownTiles", list1);
//        map.put("thrownTiles", list1);
//        map.put("index", 2);
//        list.add(map);
//
//        map = Maps.newLinkedHashMap();
//        map.put("username", "333");
//        list1 = Lists.newLinkedList();
//        list1.add(Constants.MahjongTile.eastWind.getStruct());
//        list1.add(Constants.MahjongTile.fiveBamboo.getStruct());
//        list1.add(Constants.MahjongTile.white.getStruct());
//        map.put("ownTiles", list1);
//        map.put("thrownTiles", list1);
//        map.put("index", 3);
//        list.add(map);
//
//        //加入所有玩家所有信息
//        resultMap.put("all", list);
//        result.setObject(objectMapper.writeValueAsString(resultMap));
//        System.out.println(result);

//        JsonResult result = new JsonResult();
//        result.setStatus(true);
//        result.setMessage("allocate tile");
//        Map<String, Object> map = Maps.newLinkedHashMap();
//        map.put("username", "zyw");
//        map.put("index", 2);
//        result.setObject(objectMapper.writeValueAsString(map));
//        System.out.println(result);

//        JsonResult jsonResult = new JsonResult();
//        jsonResult.setStatus(true);
//        jsonResult.setMessage("login");
//
//        Map<String, Object> map = Maps.newLinkedHashMap();
//        map.put("username", "zyw");
//        map.put("point", 123);
//        jsonResult.setObject(objectMapper.writeValueAsString(map));
//        System.out.println(jsonResult);

        JsonResult result = new JsonResult();
        result.setStatus(true);
        result.setMessage("get tile");
        result.setObject(objectMapper.writeValueAsString(Constants.MahjongTile.sixBamboo.getStruct()));
        System.out.println(result);
    }

    public void test(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult jsonResult = new JsonResult();
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

        jsonResult.setStatus(true);
        jsonResult.setMessage("register");
        session.sendMessage(new TextMessage(jsonResult.toString()));
        log.info("用户{}注册成功, session {}", username, session.getId());
    }


}

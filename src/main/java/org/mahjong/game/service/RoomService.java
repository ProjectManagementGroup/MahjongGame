package org.mahjong.game.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.mahjong.game.Constants;
import org.mahjong.game.config.SystemWebSocketHandler;
import org.mahjong.game.domain.Room;
import org.mahjong.game.domain.User;
import org.mahjong.game.dtos.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        room.setReady(false);
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
        room.setReady(false);
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
            log.error("玩家{}不在房间中", user.getUserName());
            result.setMessage("玩家不在房间中");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (!roomMap.containsKey(user.getRoom().getId())) {
            result.setMessage("房间不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //获取房间信息
        boolean b = changeRoomStatus(user.getRoom());

        result.setStatus(true);
        result.setMessage("请求成功");

        //把所有玩家的username和point都广播
        String list = user.getRoom().getPlayers().stream().map(p -> p.getInfo()).collect(Collectors.joining(",", "[", "]"));
        Map<String, String> map = Maps.newLinkedHashMap();
        map.put("list", list);
        map.put("start", Boolean.toString(b));
        result.setObject(map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(",", "{", "}")));
//        result.setObject(objectMapper.writeValueAsString(map));
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
            roomMap.put(room.getId(), room);
            log.info("玩家{}创建随机房间{}", user.getUserName(), room.getId());
            result.setStatus(true);
            result.setMessage("加入随机房间成功");
            result.setObject(room.getId() + "");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }

        room.getPlayers().add(user);
        user.setRoom(room);

        result.setStatus(true);
        result.setMessage("加入随机房间成功");
        result.setObject(room.getId() + "");
        session.sendMessage(new TextMessage(result.toString()));

        //给房间里所有人发消息，更新画面
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
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
            log.error("玩家{}已经在房间中，无法接受邀请", user.getUserName());
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

        result.setStatus(true);
        result.setMessage("加入好友房间成功");
        session.sendMessage(new TextMessage(result.toString()));

        //给房间里所有人发消息，更新画面
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
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
            log.error("玩家{}已经在房间中，无法邀请队友", user.getUserName());
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

        result.setStatus(true);
        result.setMessage("invitation");
        result.setObject(room.getId() + "");

        //给被邀请的人发消息
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
        result.setMessage("邀请成功");
        result.setObject(null);
        session.sendMessage(new TextMessage(result.toString()));

        roomMap.put(room.getId(), room);
    }

    /**
     * 不论哪位用户准备后，都调用这个函数判断游戏是否开始
     *
     * @param room
     * @return
     */

    public boolean changeRoomStatus(Room room) {
        List<User> players = room.getPlayers();
        if (players.size() < 4) {
            return false;
        }
        for (User user : players) {
            if (!user.isReady()) {
                return false;
            }
        }
        room.setReady(true);
        return true;
    }

}

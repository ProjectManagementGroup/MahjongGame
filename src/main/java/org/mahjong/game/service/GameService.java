package org.mahjong.game.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.mahjong.game.Constants;
import org.mahjong.game.config.SystemWebSocketHandler;
import org.mahjong.game.domain.Room;
import org.mahjong.game.domain.TileRequest;
import org.mahjong.game.domain.User;
import org.mahjong.game.dtos.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.*;

/**
 * 游戏控制类，负责分配麻将等
 */
@Service
@Transactional
public class GameService {

    private static Logger log = LoggerFactory.getLogger(GameService.class);

    private static ObjectMapper objectMapper = new ObjectMapper();//用于网络发包

    @Inject
    private RoomService roomService;

    @Inject
    private UserService userService;


    /**
     * 发牌
     * 返回麻将
     *
     * @return
     */
    public void allocateMahjongTile(WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误，无法加入随机房间");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            result.setMessage("玩家不在房间内");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }

        List<Constants.MahjongTile> roomList = user.getRoom().getMahjongTiles();//房间内剩余的牌
        int index = (int) (Math.random() * roomList.size());//随机发牌
        Constants.MahjongTile mahjongTile = roomList.remove(index);//从房间内移除
        if (user.getOwnTiles() == null) {
            user.setOwnTiles(Lists.newLinkedList());
        }
        user.getOwnTiles().add(mahjongTile);//加入到玩家手牌
        userService.save(user);
        log.info("成功为玩家{}发牌{}", user.getUsername(), mahjongTile.getChineseName());

        result.setStatus(true);
        result.setMessage("get tile");
        result.setObject(mahjongTile.name());
        session.sendMessage(new TextMessage(result.toString()));


        result.setMessage("allocate tile");
        result.setObject(user.getUsername());
        //给某个玩家发牌的时候要通知一下其他人，方便画页面
        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUsername())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUsername());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }
    }

    /**
     * 出牌
     */
    public void throwMahjongTile(String[] payloadArray, WebSocketSession session) throws Exception {
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
        if (user.getRoom() == null) {
            result.setMessage("玩家不在房间内");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getOwnTiles() == null) {
            result.setMessage("玩家没有手牌");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Constants.MahjongTile mahjongTile = Constants.MahjongTile.valueOf(payloadArray[1]);
        Set<Constants.MahjongTile> set = Sets.newHashSet(user.getOwnTiles());
        if (!set.contains(mahjongTile)) {
            log.error("玩家{}不含有麻将牌{}", user.getUsername(), mahjongTile.getChineseName());
            result.setMessage("玩家没有这张麻将牌");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        user.getRoom().setLastTile(mahjongTile);//更新房间的最后一张牌
        user.getOwnTiles().remove(mahjongTile);//从手牌中移除

        if (user.getThrownTiles() == null) {
            user.setThrownTiles(Lists.newLinkedList());
        }
        user.getThrownTiles().add(mahjongTile);//加入已出过的牌
        userService.save(user);
        log.info("玩家{}成功出牌{}", user.getUsername(), mahjongTile.getChineseName());

        //广播
        //给房间里所有人发消息
        result.setStatus(true);
        result.setMessage("out success");
        session.sendMessage(new TextMessage(result.toString()));

        result.setMessage("other out");
        Map<String, String> map = Maps.newLinkedHashMap();
        map.put("username", user.getUsername());
        map.put("tile", mahjongTile.name());
        result.setObject(objectMapper.writeValueAsString(map));

        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUsername())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUsername());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }
        user.getRoom().setTimerStart(DateTime.now());//计时器启动
    }

    /**
     * 胡牌请求：前台判断，如果有人胡了，就加入队列
     */
    public void win(WebSocketSession session, String t) throws Exception {
        JsonResult result = new JsonResult();
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            result.setMessage("玩家不在房间内");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        //可能是自摸胡牌,那么这个时候没有计时器
        if (user.getRoom().getTimerStart() == null) {
            processWin(user.getRoom());
            return;
        }
        //点炮胡牌
        Constants.RequestType type = Constants.RequestType.valueOf(t);
        TileRequest tileRequest = new TileRequest();
        tileRequest.setType(type);
        tileRequest.setUser(user);
        user.getRoom().getRequests().add(tileRequest);

        result.setStatus(true);
        result.setMessage(t + " request success");
        session.sendMessage(new TextMessage(result.toString()));
    }

    /**
     * 胡牌
     */
    public void processWin(Room room) throws Exception {
        String username = room.getRequests().get(0).getUser().getUsername();
        User user = userService.getUserFromUsername(username);
        //TODO:计算分数，更新玩家point值

        //广播胜利消息
        JsonResult result = new JsonResult();
        result.setStatus(true);
        result.setMessage("game end");

        Map<String, Object> resultMap = Maps.newLinkedHashMap();

        //加入胜利玩家信息
        resultMap.put("winner-username", username);
        resultMap.put("winner-index", user.getIndex());

        List<Object> list = Lists.newLinkedList();
        for (User u : room.getPlayers()) {
            Map<String, Object> map = Maps.newLinkedHashMap();
            map.put("username", u.getUsername());
            map.put("ownTiles", u.getOwnTiles());
            map.put("thrownTiles", u.getThrownTiles());
            map.put("index", u.getIndex());
            list.add(map);
        }

        //加入所有玩家所有信息
        resultMap.put("list", list);
        result.setObject(objectMapper.writeValueAsString(resultMap));

        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUsername())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUsername());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        //恢复房间和玩家的状态
        roomService.resetRoom(user.getRoom());
        user.getRoom().getPlayers().stream().forEach(p -> userService.resetUser(p));
    }

    /**
     * 胡牌
     */
    public void processTie(Room room) throws Exception {
        //广播s平局消息
        JsonResult result = new JsonResult();
        result.setStatus(true);
        result.setMessage("game tie");

        Map<String, Object> resultMap = Maps.newLinkedHashMap();

        List<Object> list = Lists.newLinkedList();
        for (User u : room.getPlayers()) {
            Map<String, Object> map = Maps.newLinkedHashMap();
            map.put("username", u.getUsername());
            map.put("ownTiles", u.getOwnTiles());
            map.put("thrownTiles", u.getThrownTiles());
            map.put("index", u.getIndex());
            list.add(map);
        }

        //加入所有玩家所有信息
        resultMap.put("list", list);
        result.setObject(objectMapper.writeValueAsString(resultMap));

        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUsername())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUsername());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        //恢复房间和玩家的状态
        roomService.resetRoom(room);
        room.getPlayers().stream().forEach(p -> userService.resetUser(p));
    }

    /**
     * 游戏开始
     * 只对第一个人发牌
     *
     * @param room
     * @throws Exception
     */
    public void gameStart(Room room) throws Exception {
        room.setPlaying(true);
        room.setIndex(0);

        JsonResult result = new JsonResult();
        result.setStatus(true);
        result.setMessage("game start");
        //首先广播信息，通知玩家游戏已经开始
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUsername())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUsername());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        result.setStatus(true);
        result.setMessage("game start allocate");

        //给第一个玩家发14张牌，其他玩家13张牌
        List<Constants.MahjongTile> roomList = room.getMahjongTiles();//房间内剩余的牌
        for (User user : room.getPlayers()) {
            int range = 13;
            if (room.getPlayerIndex(user.getUsername()) == 0) {
                range = 14;
            }
            for (int i = 0; i < range; i++) {
                int index = (int) (Math.random() * roomList.size());//随机发牌
                Constants.MahjongTile mahjongTile = roomList.remove(index);//从房间内移除
                if (user.getOwnTiles() == null) {
                    user.setOwnTiles(Lists.newLinkedList());
                }
                user.getOwnTiles().add(mahjongTile);//加入到玩家手牌
                log.info("成功为玩家{}发牌{}", user.getUsername(), mahjongTile.getChineseName());
            }
            userService.save(user);
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(user.getUsername());
            Map<String, Object> map = Maps.newLinkedHashMap();
            map.put("own-index", user.getIndex());
            map.put("own-tiles", user.getOwnTiles());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        //下面等待第一个玩家出牌
    }

    /**
     * 新的回合开始
     * 对当前轮到的人发牌
     * 更新index
     *
     * @param room
     * @throws Exception
     */
    public void nextRound(Room room) throws Exception {
        room.setTimerStart(null); //下面等待该用户出牌，所以计时器设为空

        //先判断是不是有人胡牌了
        int index = processBeforeNewRound(room);//通知某个玩家碰牌或者吃牌成功
        if (index == -2) {//说明有人胡了
            processWin(room);
        } else if (index == -1) {//如果没有要抢，那么按照正常顺序开会时发牌
            boolean b = isTie(room);
            if (b) {
                processTie(room);
                return;
            }
            index = room.getIndex() + 1;
            if (index == 4) {
                index = 0;
            }
            room.setIndex(index);
            User user = room.getPlayers().get(index);
            //给第index个玩家发牌
            WebSocketSession session = SystemWebSocketHandler.sessionsMap.get(user.getUsername());
            allocateMahjongTile(session);
        } else {
            //有人要碰牌或者吃牌或者杠
            room.setIndex(index);
            //如果是杠，那么已经给玩家发了牌；如果是碰牌或者吃牌，那么也相当于也给玩家发了牌。所以无论如何，下面就等待用户出牌
        }
    }

    private boolean isTie(Room room) {
        return room.getMahjongTiles().isEmpty();
    }

    /**
     * 碰牌，只记录下请求，不作处理
     *
     * @param session
     * @throws Exception
     */
    public void bumpOrEatOrKong(WebSocketSession session, String t) throws Exception {
        JsonResult result = new JsonResult();
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            result.setMessage("玩家不在房间内");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Constants.RequestType type = Constants.RequestType.valueOf(t);
        TileRequest tileRequest = new TileRequest();
        tileRequest.setType(type);
        tileRequest.setUser(user);
        user.getRoom().getRequests().add(tileRequest);

        result.setStatus(true);
        result.setMessage(t + " request success");
        session.sendMessage(new TextMessage(result.toString()));
    }

    private int processBeforeNewRound(Room room) throws Exception {
        //情况1：碰 吃
        //情况2：吃 碰
        //情况3：杠 吃
        //情况3：有人胡牌
        //所以最多一个碰一个吃
        List<TileRequest> list = room.getRequests();
        if (list.size() == 0) {
            return -1;
        }
        Collections.sort(list, (a, b) -> b.getType().getRank() - a.getType().getRank());
        //是不是胡牌
        if (list.get(0).getType() == Constants.RequestType.win) {
            return -2;
        }
        //不是胡牌那么判断碰牌和吃牌和杠
        String username = list.get(0).getUser().getUsername();
        int index = room.getPlayerIndex(username);
        WebSocketSession session = SystemWebSocketHandler.sessionsMap.get(username);
        processBumpOrEatOrKong(session, room, list.get(0).getType().name());
        return index;
    }

    public void kongDark(WebSocketSession session) throws Exception {
        //暗杠的情况是：轮到这个用户抓牌，这个用户得到了暗杠，所以发送暗杠请求，就轮到这个用户抓牌了
        JsonResult result = new JsonResult();
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            result.setMessage("玩家不在房间内");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Room room = user.getRoom();
        if (room.getIndex() != user.getIndex()) {
            result.setMessage("未轮到用户抓牌");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }

        //首先向其他玩家广播有人暗杠的消息
        result.setMessage("other dark kong success");
        Map<String, String> map = Maps.newLinkedHashMap();
        map.put("username", user.getUsername());
        result.setObject(map.toString());
        broadcastSpecificMessage(user.getRoom(), result.toString(), user.getId());

        //再给这个用户发一张牌
        allocateMahjongTile(session);

    }

    private void processBumpOrEatOrKong(WebSocketSession session, Room room, String type) throws Exception {
        JsonResult result = new JsonResult();

        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            result.setMessage("玩家不在房间内");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        user.getOwnTiles().add(room.getLastTile());//加入到玩家手牌

        result.setStatus(true);
        //向其他玩家广播
        result.setMessage(type + " success");
        Map<String, Object> map = Maps.newLinkedHashMap();
        map.put("username", user.getUsername());
        map.put("index", user.getIndex());
        map.put("type", type);
        map.put("tile", room.getLastTile());
        result.setObject(objectMapper.writeValueAsString(map));

        broadcastSpecificMessage(user.getRoom(), result.toString(), user.getId());

        //如果是明杠给这个玩家发牌
        if (Constants.RequestType.kong.name().equals(type)) {
            allocateMahjongTile(session);
        }
    }

    private void broadcastSpecificMessage(Room room, String json, Long userId) throws Exception {
        for (User u : room.getPlayers()) {
            if (u.getId() == userId) {
                continue;
            }
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUsername())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUsername());
            socketSession.sendMessage(new TextMessage(json));
        }
    }
}

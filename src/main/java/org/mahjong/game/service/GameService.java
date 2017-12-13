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
        log.info("成功为玩家{}发牌{}", user.getUserName(), mahjongTile.getChineseName());

        result.setStatus(true);
        result.setMessage("get tile");
        result.setObject(mahjongTile.name());
        session.sendMessage(new TextMessage(result.toString()));


        result.setMessage("allocate tile");
        result.setObject(user.getUserName());
        //给某个玩家发牌的时候要通知一下其他人，方便画页面
        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
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
            log.error("玩家{}不含有麻将牌{}", user.getUserName(), mahjongTile.getChineseName());
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
        log.info("玩家{}成功出牌{}", user.getUserName(), mahjongTile.getChineseName());

        //广播
        //给房间里所有人发消息
        result.setStatus(true);
        result.setMessage("out success");
        session.sendMessage(new TextMessage(result.toString()));

        result.setMessage("other out");
        Map<String, String> map = Maps.newLinkedHashMap();
        map.put("username", user.getUserName());
        map.put("tile", mahjongTile.name());
        result.setObject(objectMapper.writeValueAsString(map));

        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }
        user.getRoom().setTimerStart(DateTime.now());//计时器启动
    }

    /**
     * 前台判断，如果有人胡了，那么调用这个函数
     */
    public void win(WebSocketSession session) throws Exception {
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
        //TODO:计算分数，更新玩家point值

        //广播胜利消息
        result.setStatus(true);
        result.setMessage("win");
        result.setObject("分数");
        session.sendMessage(new TextMessage(result.toString()));

        result.setMessage("lose");
        result.setObject("分数");
        session.sendMessage(new TextMessage(result.toString()));

        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        //回复房间和玩家的状态
        roomService.resetRoom(user.getRoom());
        user.getRoom().getPlayers().stream().forEach(p -> userService.resetUser(p));
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
        User user = room.getPlayers().get(0);

        JsonResult result = new JsonResult();
        result.setStatus(true);
        result.setMessage("game start");
        //首先广播信息，通知玩家游戏已经开始，这个消息玩家可能已经收到，如果收到了就忽略
        for (User u : user.getRoom().getPlayers()) {
            if (u.getId() == user.getId()) {
                continue;
            }
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        //给第一个玩家发牌
        WebSocketSession session = SystemWebSocketHandler.sessionsMap.get(user.getUserName());
        //TODO:暂时不考虑玩家掉线情况
        allocateMahjongTile(session);
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
        int index = controlBumpOrEat(room);//通知某个玩家碰牌或者吃牌成功
        room.setTimerStart(null); //下面等待该用户出牌，所以计时器设为空
        if (index != -1) {//说明有人吃牌或者碰牌
            room.setIndex(index);//重新设置下一个人
        } else {//如果没有要抢，那么按照正常顺序开会时发牌
            index = room.getIndex() + 1;
            if (index == 4) {
                index = 0;
            }
            room.setIndex(index);
            User user = room.getPlayers().get(index);
            //给第index个玩家发牌
            WebSocketSession session = SystemWebSocketHandler.sessionsMap.get(user.getUserName());
            //TODO:暂时不考虑玩家掉线情况
            allocateMahjongTile(session);
        }

    }

    /**
     * 碰牌，只记录下请求，不作处理
     *
     * @param session
     * @throws Exception
     */
    public void bumpOrEat(WebSocketSession session, String t) throws Exception {
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
        Constants.RequestType type = Constants.RequestType.valueOf(t);
        TileRequest tileRequest = new TileRequest();
        tileRequest.setType(type);
        tileRequest.setUser(user);
        user.getRoom().getRequests().add(tileRequest);

        result.setStatus(true);
        result.setMessage(t + " request success");
        session.sendMessage(new TextMessage(result.toString()));
    }

    private int controlBumpOrEat(Room room) throws Exception {
        //情况1：碰 吃
        //情况2：吃 碰
        //所以最多一个碰一个吃
        List<TileRequest> list = room.getRequests();
        if (list.size() == 0) {
            return -1;
        }
        Collections.sort(list, (a, b) -> b.getType().getRank() - a.getType().getRank());
        String username = list.get(0).getUser().getUserName();
        int index = room.getPlayerIndex(username);
        WebSocketSession session = SystemWebSocketHandler.sessionsMap.get(username);
        //TODO:暂时不考虑掉线情况
        processBumpOrEat(session, room, list.get(0).getType().name());
        return index;
    }

    private void processBumpOrEat(WebSocketSession session, Room room, String type) throws Exception {
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
        user.getOwnTiles().add(room.getLastTile());//加入到玩家手牌

        result.setStatus(true);
        result.setMessage(type + " success");
        result.setObject(room.getLastTile().name());
        session.sendMessage(new TextMessage(result.toString()));


        //向其他玩家广播
        result.setMessage("other " + type + " success");
        Map<String, String> map = Maps.newLinkedHashMap();
        map.put("username", user.getUserName());
        map.put("type", type);
        result.setObject(map.toString());

        broadcastSpecificMessage(user.getRoom(), result.toString(), user.getId());
    }

    private void broadcastSpecificMessage(Room room, String json, Long userId) throws Exception {
        for (User u : room.getPlayers()) {
            if (u.getId() == userId) {
                continue;
            }
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getUserName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getUserName());
            socketSession.sendMessage(new TextMessage(json));
        }
    }
}

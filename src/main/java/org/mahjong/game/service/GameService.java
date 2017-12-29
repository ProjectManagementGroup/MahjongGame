package org.mahjong.game.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.mahjong.game.Constants;
import org.mahjong.game.config.SystemWebSocketHandler;
import org.mahjong.game.domain.FriendRelation;
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
        log.info("成功为玩家{} <发牌> {}，session {}，房间id{}", user.getName(), mahjongTile.getChineseName(), session.getId(), user.getRoom().getId());

        result.setStatus(true);
        result.setMessage("get tile");
        result.setObject(objectMapper.writeValueAsString(mahjongTile.getStruct()));
        session.sendMessage(new TextMessage(result.toString()));


        result.setMessage("allocate tile");
        Map<String, Object> map = Maps.newLinkedHashMap();
        map.put("name", user.getName());
        map.put("gameid", user.getGameid());
        result.setObject(objectMapper.writeValueAsString(map));
        //给某个玩家发牌的时候要通知一下其他人，方便画页面
        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            //不给发牌人发消息
            if (u.getId() == user.getId()) {
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }
    }

    /**
     * 出牌
     */
    public void throwMahjongTile(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        if (payloadArray.length != 3) {
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
        String type = payloadArray[1];
        String value = payloadArray[2];
        Constants.MahjongTile mahjongTile = Constants.MahjongTile.getTileByTypeAndNumber(Constants.MahjongType.valueOf(type), Integer.parseInt(value));
        if (mahjongTile == null) {
            result.setMessage("不存在这张牌");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        Set<Constants.MahjongTile> set = Sets.newHashSet(user.getOwnTiles());
        if (!set.contains(mahjongTile)) {
            log.error("玩家{}不含有麻将牌{}", user.getName(), mahjongTile.getChineseName());
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
        log.info("玩家{}成功 <出牌> {}，session {}，房间id{}", user.getName(), mahjongTile.getChineseName(), session.getId(), user.getRoom().getId());

        //广播
        //给房间里所有人发消息
        result.setStatus(true);
        result.setMessage("out success");
        session.sendMessage(new TextMessage(result.toString()));

        result.setMessage("other out");
        Map<String, Object> map = Maps.newLinkedHashMap();
        map.put("name", user.getName());
        map.put("gameid", user.getGameid());
        map.put("tile", mahjongTile.getStruct());
        result.setObject(objectMapper.writeValueAsString(map));

        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            //不给出牌人发消息
            if (u.getId() == user.getId()) {
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }
        user.getRoom().setTimerStart(DateTime.now());//计时器启动
    }


    /**
     * 发言，只有在房间里才能发言
     */
    public void speak(String[] payloadArray, WebSocketSession session) throws Exception {
        JsonResult result = new JsonResult();
        if (payloadArray.length != 2) {
            result.setMessage("请求信息错误");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        User user = userService.getUserFromSession(session);
        if (user == null) {
            result.setMessage("用户信息错误, 用户不存在");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }
        if (user.getRoom() == null) {
            result.setMessage("玩家不在房间内");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }


        //广播
        //给房间里所有人发消息
        result.setStatus(true);
        result.setMessage("speak");

        Map<String, Object> map = Maps.newLinkedHashMap();
        map.put("speaker", user.getName());
        map.put("gameid", user.getGameid());
        map.put("content", payloadArray[1]);
        result.setObject(objectMapper.writeValueAsString(map));

        for (User u : user.getRoom().getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }
        log.info("玩家{}发言，session {}，房间id{}，发言内容{}", user.getName(), session.getId(), user.getRoom().getId(), payloadArray[1]);

    }

    /**
     * 胡牌请求：前台判断，如果有人胡了，就加入队列
     */
    public void win(String[] payloadArray, WebSocketSession session, String t) throws Exception {
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
        Constants.MahjongTile tile = Constants.MahjongTile.getTileByTypeAndNumber(Constants.MahjongType.getTypeById(Integer.parseInt(payloadArray[1])), Integer.parseInt(payloadArray[2]));

        //可能是自摸胡牌,那么这个时候没有计时器
        if (user.getRoom().getTimerStart() == null) {
            log.info("玩家{}自摸胡牌，session {}，房间id{}", user.getName(), session.getId(), user.getRoom().getId());
            processWin(tile, user.getRoom(), user);
            return;
        }
        //点炮胡牌
        Constants.RequestType type = Constants.RequestType.valueOf(t);
        TileRequest tileRequest = new TileRequest();
        tileRequest.setType(type);
        tileRequest.setUser(user);
        tileRequest.setTile0(tile);//胡的牌是什么
        user.getRoom().getRequests().add(tileRequest);
        log.info("玩家{}点炮胡牌，session {}，房间id{}", user.getName(), session.getId(), user.getRoom().getId());

        result.setStatus(true);
        result.setMessage(t + " request success");
        session.sendMessage(new TextMessage(result.toString()));

    }

    /**
     * 胡牌
     */
    public void processWin(Constants.MahjongTile tile, Room room, User winner) throws Exception {

        //广播胜利消息
        JsonResult result = new JsonResult();
        result.setStatus(true);
        result.setMessage("game end");

        Map<String, Object> resultMap = Maps.newLinkedHashMap();

        //加入胜利玩家信息
        resultMap.put("winnerName", winner.getName());
        resultMap.put("winnerIndex", winner.getGameid());
        resultMap.put("winTile", tile.getStruct());
        resultMap.put("ownTiles", winner.getJsonOwnTileLists());
//        List<Object> list = Lists.newLinkedList();
//        for (User u : room.getPlayers()) {
//            Map<String, Object> map = Maps.newLinkedHashMap();
//            map.put("name", u.getName());
//            if (u.getId() == winner.getId()) {
//                u.setPoint(u.getPoint() + 50);
//            } else {
//                u.setPoint(u.getPoint() - 50);
//            }
//            map.put("point", u.getPoint());
//            map.put("thrownTiles", u.getJsonThrownTileLists());
//            map.put("ownTiles", u.getJsonOwnTileLists());
//            map.put("gameid", u.getGameid());
//            list.add(map);
//        }

        //加入所有玩家所有信息
        result.setObject(objectMapper.writeValueAsString(resultMap));

        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        //恢复房间和玩家的状态
        roomService.resetRoom(room);
        //赢家index=0
        roomService.adjustUserIndex(room, winner);
        room.getPlayers().stream().forEach(p -> userService.resetUser(p));
        log.info("房间id{}，结束游戏，胜者{}", room.getId(), winner.getName());

    }

    /**
     * 荒牌
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
            map.put("name", u.getName());
            map.put("ownTiles", u.getOwnTiles());
            map.put("thrownTiles", u.getJsonThrownTileLists());
            map.put("ownTiles", u.getJsonOwnTileLists());
            map.put("gameid", u.getGameid());
            list.add(map);
        }

        //加入所有玩家所有信息
        resultMap.put("all", list);
        result.setObject(objectMapper.writeValueAsString(resultMap));

        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
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
        log.info("房间id{}，游戏开始", room.getId());

        room.setPlaying(true);
        room.setIndex(0);

        JsonResult result = new JsonResult();
        result.setStatus(true);
        result.setMessage("game start");
        //首先广播信息，通知玩家游戏已经开始
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            socketSession.sendMessage(new TextMessage(result.toString()));
        }

        result.setStatus(true);
        result.setMessage("game start allocate");

        //给第一个玩家发14张牌，其他玩家13张牌
        List<Constants.MahjongTile> roomList = room.getMahjongTiles();//房间内剩余的牌
        for (User user : room.getPlayers()) {
            int range = 13;
            if (room.getPlayerIndex(user.getName()) == 0) {
                range = 14;
            }
            for (int i = 0; i < range; i++) {
                int index = (int) (Math.random() * roomList.size());//随机发牌
                Constants.MahjongTile mahjongTile = roomList.remove(index);//从房间内移除
                if (user.getOwnTiles() == null) {
                    user.setOwnTiles(Lists.newLinkedList());
                }
                user.getOwnTiles().add(mahjongTile);//加入到玩家手牌
                log.info("成功为玩家{} <发牌> {}", user.getName(), mahjongTile.getChineseName());
            }
            userService.save(user);
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(user.getName());
            Map<String, Object> map = Maps.newLinkedHashMap();
            map.put("ownIndex", user.getGameid());
            map.put("ownTiles", user.getJsonOwnTileLists());
            result.setObject(objectMapper.writeValueAsString(map));
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
            List<TileRequest> list = room.getRequests();
            User user = list.get(0).getUser();
            processWin(list.get(0).getTile0(), room, user);
        } else if (index == -1) {//如果没有要抢，那么按照正常顺序开会时发牌
            log.info("房间id{}，新一轮开始", room.getId());
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
            WebSocketSession session = SystemWebSocketHandler.sessionsMap.get(user.getName());
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
     * 碰、吃牌，只记录下请求，不作处理
     *
     * @param session
     * @throws Exception
     */
    public void bumpOrEatOrKong(String[] payloadArray, WebSocketSession session, String t) throws Exception {
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
        tileRequest.setTile0(user.getRoom().getLastTile());

        //设置另外两张牌都是什么
        if (type == Constants.RequestType.eat) {
            int number1 = Integer.parseInt(payloadArray[1]);
            int number2 = Integer.parseInt(payloadArray[2]);
            tileRequest.setTile1(Constants.MahjongTile.getTileByTypeAndNumber(user.getRoom().getLastTile().getType(), number1));
            tileRequest.setTile2(Constants.MahjongTile.getTileByTypeAndNumber(user.getRoom().getLastTile().getType(), number2));
        } else if (type == Constants.RequestType.bump) {
            tileRequest.setTile1(user.getRoom().getLastTile());
            tileRequest.setTile2(user.getRoom().getLastTile());
        }

        user.getRoom().getRequests().add(tileRequest);
        result.setStatus(true);
        result.setMessage(t + " request success");
        session.sendMessage(new TextMessage(result.toString()));

        log.info("用户{}，session {} 发送了 <{}> 的请求, 最后一张牌是 {}, 其余两张牌是 {} 和 {}", user.getName(), session.getId(), t, tileRequest.getTile0(), tileRequest.getTile1(), tileRequest.getTile2());
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
            log.info("房间id{}，新一轮开始，有人点炮胡牌", room.getId());
            return -2;
        }
        //不是胡牌那么判断碰牌和吃牌和杠
        String name = list.get(0).getUser().getName();
        int index = room.getPlayerIndex(name);
        WebSocketSession session = SystemWebSocketHandler.sessionsMap.get(name);
        processBumpOrEatOrKong(list.get(0), session, room, list.get(0).getType().name());
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
        if (room.getIndex() != user.getGameid()) {
            result.setMessage("未轮到用户抓牌");
            session.sendMessage(new TextMessage(result.toString()));
            return;
        }

        //首先向其他玩家广播有人暗杠的消息
        result.setMessage("other dark kong success");
        Map<String, String> map = Maps.newLinkedHashMap();
        map.put("name", user.getName());
        result.setObject(map.toString());
        broadcastSpecificMessage(user.getRoom(), result.toString(), user.getId());

        //再给这个用户发一张牌
        allocateMahjongTile(session);

    }

    private void processBumpOrEatOrKong(TileRequest tileRequest, WebSocketSession session, Room room, String type) throws Exception {
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
        result.setMessage("cut success");
        Map<String, Object> map = Maps.newLinkedHashMap();
        map.put("name", user.getName());
        map.put("gameid", user.getGameid());
        map.put("type", type);

        List<Object> list = Lists.newLinkedList();
        list.add(room.getLastTile().getStruct());
        list.add(tileRequest.getTile1().getStruct());
        list.add(tileRequest.getTile2().getStruct());
        map.put("tile", list);

        result.setObject(objectMapper.writeValueAsString(map));
        log.info("房间id{}，用户{}/{} ,{} 成功，三张牌分别是{}、{}、{}", room.getId(), user.getName(), session.getId(), type, room.getLastTile().getChineseName(), tileRequest.getTile1().getChineseName(), tileRequest.getTile2().getChineseName());

        broadcastSpecificMessage(user.getRoom(), result.toString(), user.getId());

        //如果是明杠给这个玩家发牌
        if (Constants.RequestType.kong.name().equals(type)) {
            allocateMahjongTile(session);
        }
    }

    private void broadcastSpecificMessage(Room room, String json, Long userId) throws Exception {
        for (User u : room.getPlayers()) {
            if (!SystemWebSocketHandler.sessionsMap.containsKey(u.getName())) {//下线的人暂时不管
                continue;
            }
            WebSocketSession socketSession = SystemWebSocketHandler.sessionsMap.get(u.getName());
            socketSession.sendMessage(new TextMessage(json));
        }
    }

}

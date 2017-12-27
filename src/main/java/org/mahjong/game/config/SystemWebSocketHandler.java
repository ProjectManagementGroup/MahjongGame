package org.mahjong.game.config;

import org.mahjong.game.dtos.JsonResult;
import org.mahjong.game.service.GameService;
import org.mahjong.game.service.RoomService;
import org.mahjong.game.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.socket.*;

import javax.inject.Inject;
import java.util.*;

@CrossOrigin
@Component
public class SystemWebSocketHandler implements WebSocketHandler {
    private final Logger log = LoggerFactory.getLogger(SystemWebSocketHandler.class);

    //用户所有的session，不允许同一个用户同时有不同的session
    public static final List<WebSocketSession> sessions = new LinkedList<>();

    //username -> session
    public static final Map<String, WebSocketSession> sessionsMap = new LinkedHashMap<>();

    @Inject
    private RoomService roomService;

    @Inject
    private UserService userService;

    @Inject
    private GameService gameService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        log.info("链接webSocket成功");
//        if (session.getAttributes().containsKey("webUser")) {
//            sessions.add(session);
//            sessionsMap.put(session.getAttributes().get("webUser") + "", session);//key是userName
//            System.out.println("----------------");
//            System.out.println(session.getAttributes().get("webUser") + " 成功登陆并分配了session " + session.getId());
//            System.out.println("----------------");
//        } else {
//        System.out.println("暂时还没有登陆！");//应该只有这一条会执行
//        }
        log.info("session {} 连接成功", session.getId());
        session.sendMessage(new TextMessage("success connect"));
    }


    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
        String payload = webSocketMessage.getPayload().toString();
        String[] payloadArray = payload.split("\\|");
        if (payloadArray.length < 1) {
            session.sendMessage(new TextMessage(new JsonResult("消息格式错误").toString()));
            return;
        }
        switch (payloadArray[0]) {
            /**
             * 登录消息格式
             * login|username|password
             * 登录成功后session里面存着username
             */
            case "test":
                roomService.test(payloadArray, session);
                break;
            case "login":
                userService.login(payloadArray, session);
                break;
            /**
             * 注册消息格式
             * register|username|password
             * 注册成功后需要登录
             */
            case "register"://有人发送了消息,也可能是文件
                userService.register(payloadArray, session);
                break;
            /**
             * 发送准备请求
             * ready|
             * 加入房间后可以准备
             */
            case "ready"://有人发送了消息,也可能是文件
                userService.setReady(session);
                break;
            /**
             * 随机加入房间游戏
             * join-random|
             */
            case "join-random":
                roomService.joinRandomRoom(session);
                break;
            /**
             * 创建好友房间并邀请三个好友
             * invite|f1|f2|f3
             * friendx是朋友的用户名
             */
            case "invite":
                roomService.sendInvitationAndCreateRoom(payloadArray, session);
                break;
            /**
             * 用户接受邀请加入好友房间
             * accept|roomId
             * 注册成功后需要登录
             */
            case "accept":
                roomService.joinFriendRoom(payloadArray, session);
                break;
            /**
             * 用户出牌
             * out|tile
             * tile指麻将牌的名字
             */
            case "out":
                gameService.throwMahjongTile(payloadArray, session);
                break;
            /**
             * 用户暗杠
             * kong-dark
             */
            case "kong-dark":
                gameService.kongDark(session);
                break;
            /**
             * 用户明杠
             * kong-bright|
             */
            case "kong-bright":
                gameService.bumpOrEatOrKong(session, "kong");
                break;
            /**
             * 用户退房
             * exit|
             */
            case "exit":
                roomService.exit(session);
                break;
            /**
             * 用户胡牌
             * win|
             */
            case "win":
                gameService.win(session, "win");
                break;
            /**
             * 碰
             * bump|
             */
            case "bump":
                gameService.bumpOrEatOrKong(session, "bump");
                break;
            /**
             * 吃
             * eat|
             */
            case "eat":
                gameService.bumpOrEatOrKong(session, "eat");
                break;
            default:
                log.error("请求消息错误");
                session.sendMessage(new TextMessage(new JsonResult("消息格式错误").toString()));
                break;
        }


    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable t) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage("close!"));
            session.close();
        }
        log.debug("WebSocket出错！");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus cs) throws Exception {
        Iterator<WebSocketSession> it = sessions.iterator();
        while (it.hasNext()) {
            WebSocketSession s = it.next();
            if (s == session) {
                it.remove();
                sessionsMap.remove(s.getAttributes().get("webUser") + "");
                continue;
            }
            if (!s.isOpen()) {
                it.remove();
                sessionsMap.remove(s.getAttributes().get("webUser") + "");
            }
        }
        log.debug("WebSocket关闭！");
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
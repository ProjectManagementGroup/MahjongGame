package org.mahjong.game.config;

import org.mahjong.game.service.AESKitService;
import org.mahjong.game.service.ApplyService;
import org.mahjong.game.service.ChatingService;
import org.mahjong.game.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import javax.inject.Inject;
import java.util.*;


@Component
public class SystemWebSocketHandler implements WebSocketHandler {
    private final Logger log = LoggerFactory.getLogger(SystemWebSocketHandler.class);
    public static final List<WebSocketSession> sessions = new LinkedList<>();
    public static final Map<String, WebSocketSession> sessionsMap = new LinkedHashMap<>();
    public static final String symkey = "2017051820170518";
    @Inject
    private AESKitService aesKitService;

    @Inject
    private LoginService loginService;

    @Inject
    private ChatingService chatingService;

    @Inject
    private ApplyService applyService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("链接webSocket成功");
        if (session.getAttributes().containsKey("webUser")) {
            sessions.add(session);
            sessionsMap.put(session.getAttributes().get("webUser") + "", session);//key是userName
            System.out.println("----------------");
            System.out.println(session.getAttributes().get("webUser") + " 成功登陆并分配了session " + session.getId());
            System.out.println("----------------");
        } else {
            System.out.println("暂时还没有登陆！");//应该只有这一条会执行
        }
        session.sendMessage(new TextMessage("服务端链接成功！"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
//        TextMessage returnMessage = new TextMessage(wss.getId() + ":" + wsm.getPayload());
        String payload = webSocketMessage.getPayload() + "";
        //先解密
        payload = aesKitService.Decrypt(payload, symkey);
        String[] payloadArray = payload.split("\\|");
        switch (payloadArray[0]) {
            case "login":  /**
             * 登录消息格式
             * login|userName|password
             */
                String userName = payloadArray[1];
                String password = payloadArray[2];
                System.out.println("用户 " + userName + " 准备登陆");
                loginService.login(userName, password, session);
                break;
            case "charMessage"://有人发送了消息,也可能是文件
                chatingService.chating(payloadArray, session.getAttributes().get("webUser") + "");
                System.out.println(session.getAttributes().get("webUser") + "准备和" + payloadArray[2] + "聊天");
                break;
            case "search":
                applyService.searchFriend(payloadArray[1], session);
                break;
            case "applyFriend":
                applyService.applyFriend(payloadArray[1], session);
                break;
            case "accept":
                applyService.acceptOrRefuseApply(payloadArray);
                break;
            case "refuse":
                applyService.acceptOrRefuseApply(payloadArray);
                break;
            case "applyPublicKey":
                applyService.applyPublicKey(payloadArray[1], session);
                break;
            default:
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
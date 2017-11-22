package org.mahjong.game.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.mahjong.game.config.SystemWebSocketHandler;

import javax.inject.Inject;
import javax.transaction.Transactional;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Service
@Transactional
public class ChatingService {


    @Inject
    private AESKitService aesKitService;

    //有人发送了消息
    public boolean chating(String[] payloadArray, String userName) throws Exception {
        String message = payloadArray[1];
        String friendName = payloadArray[2];
        String flag = payloadArray[3];
        if (SystemWebSocketHandler.sessionsMap.containsKey(friendName)) {//朋友在线
            WebSocketSession webSocketSessionHim = SystemWebSocketHandler.sessionsMap.get(friendName);
            //判断是图片还是文本文件还是普通消息
            if (!flag.equals("1")) {//不是普通消息
                int pos = message.indexOf("data:") + 5;
                int pos1 = message.indexOf(";");//data:image/png;base64,iVBORw0K
                int pos2 = message.substring(pos, pos1).indexOf("/");
                if (pos2 > 0 && message.substring(pos, message.indexOf("/")).equals("image")) {
                    flag = "2";
                } else {//文本文件
                    pos = message.indexOf("base64,") + 7;
                    message = message.substring(pos, message.length());
                    flag = "3";
                }
            }
            //如果是普通消息，那么什么都不用变
            webSocketSessionHim.sendMessage(new TextMessage(aesKitService.Encrypt("chating" + "|" + message + "|" + userName + "|" + flag, SystemWebSocketHandler.symkey)));
        }
        return true;
    }
}

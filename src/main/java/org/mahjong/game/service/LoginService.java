package org.mahjong.game.service;

import org.apache.commons.codec.binary.Base64;
import org.mahjong.game.repository.WebUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.mahjong.game.config.SystemWebSocketHandler;
import org.mahjong.game.domain.WebUser;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Service
@Transactional
public class LoginService {

    @Inject
    private WebUserRepository webUserRepository;

    @Inject
    private RSAService rsaService;

    @Inject
    private AESKitService aesKitService;

    //只有登陆成功才分配session
    public void login(String userName, String password, WebSocketSession session) throws Exception {
        Optional<WebUser> _user = webUserRepository.findOneByUserName(userName);
        if (_user.isPresent()) {//这说明是登陆
            WebUser webUser = _user.get();
            if (webUser.getPassword().equals(password)) {
                session.getAttributes().put("webUser", userName);
                SystemWebSocketHandler.sessions.add(session);
                SystemWebSocketHandler.sessionsMap.put(userName, session);//key是userName
                System.out.println("----------------");
                System.out.println(userName + " 成功登陆并分配了session " + session.getId());
                System.out.println("----------------");
                KeyPair keyPair = rsaService.generateKeyForClient();
                RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

                session.sendMessage(new TextMessage(aesKitService.Encrypt("login success|" + new String(Base64.encodeBase64(publicKey.getEncoded())) + "|" + new String(Base64.encodeBase64(privateKey.getEncoded())), SystemWebSocketHandler.symkey)));
                //覆盖原公钥私钥
                webUser.setPublicKeyGetEncoded(new String(Base64.encodeBase64(publicKey.getEncoded())));
                webUserRepository.save(webUser);
            } else {
                session.sendMessage(new TextMessage(aesKitService.Encrypt("login failure", SystemWebSocketHandler.symkey)));
            }
        } else {//说明这是注册
            WebUser webUser = new WebUser();
            webUser.setPassword(password);
            webUser.setUserName(userName);

            session.getAttributes().put("webUser", userName);
            SystemWebSocketHandler.sessions.add(session);
            SystemWebSocketHandler.sessionsMap.put(userName, session);//key是userName
            System.out.println("----------------");
            System.out.println(userName + " 成功注册并分配了session " + session.getId());
            System.out.println("----------------");

            //同时还要把他的公钥，私钥要发过去,还要数据库存公钥
            KeyPair keyPair = rsaService.generateKeyForClient();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            session.sendMessage(new TextMessage(aesKitService.Encrypt("register success|" + new String(Base64.encodeBase64(publicKey.getEncoded())) + "|" + new String(Base64.encodeBase64(privateKey.getEncoded())), SystemWebSocketHandler.symkey)));
            webUser.setPublicKeyGetEncoded(new String(Base64.encodeBase64(publicKey.getEncoded())));
            webUserRepository.save(webUser);
        }
    }
}

package org.mahjong.game.service;

import org.mahjong.game.repository.FriendRelationRepository;
import org.mahjong.game.repository.WebUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.mahjong.game.config.SystemWebSocketHandler;
import org.mahjong.game.domain.FriendRelation;
import org.mahjong.game.domain.WebUser;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Created by zhaoyawen on 2017/5/16.
 */
@Service
@Transactional
public class ApplyService {

    /**
     * 针对发送好友请求的约定：
     * 一个人发送多条请求，在数据库里合并成一条
     * 如果两个人不是朋友，那么双方都可以发送许多请求，在数据库里其实就是两条请求，以第一个人的接受或者拒绝来决定是不是朋友
     * 如果两个人是朋友，那么就无法发送好友请求了，数据库里也只有一条数据
     */


    @Inject
    private WebUserRepository webUserRepository;

    @Inject
    private FriendRelationRepository friendRelationRepository;

    @Inject
    private AESKitService aesKitService;

    //如果已经是好友那就flag=true
    public void searchFriend(String friendName, WebSocketSession session) throws Exception {
        Optional<WebUser> _webUser = webUserRepository.findOneByUserName(friendName);
        String userName = session.getAttributes().get("webUser") + "";
        if (!_webUser.isPresent()) {//搜索的用户不存在
            session.sendMessage(new TextMessage(aesKitService.Encrypt("search failure!|" + "whatever",SystemWebSocketHandler.symkey)));
        } else {//搜索的用户存在，看是不是朋友
            Optional<FriendRelation> _friendRelation = friendRelationRepository.findOneByWebUser1AndWebUser2(userName, friendName);
            if (_friendRelation.isPresent() && _friendRelation.get().getFlag() == 0) {//已经是朋友
                session.sendMessage(new TextMessage(aesKitService.Encrypt("search success!|" + "true",SystemWebSocketHandler.symkey)));
            } else {//这种关系可能不存在 或者 对方发送了请求，那无所谓，都是搜索成功
                session.sendMessage(new TextMessage(aesKitService.Encrypt("search success!|" + "false",SystemWebSocketHandler.symkey)));
            }
        }
    }

    public boolean applyFriend(String friendName, WebSocketSession session) throws Exception {
        String userName = session.getAttributes().get("webUser") + "";
        Optional<FriendRelation> _friendRelation = friendRelationRepository.findOneByWebUser1AndWebUser2(userName, friendName);
        if (!_friendRelation.isPresent()) {//说明双方都没有发送过请求，更不是朋友
            FriendRelation friendRelation = new FriendRelation();
            friendRelation.setFlag(1);
            friendRelation.setWebUser1(webUserRepository.findOneByUserName(userName).get());
            friendRelation.setWebUser2(webUserRepository.findOneByUserName(friendName).get());
            friendRelationRepository.save(friendRelation);
        } else {
            //到了这里，说明肯定不是朋友。。。。。
            Optional<FriendRelation> _friendRelation1 = friendRelationRepository.findOneByWebUser1AndWebUser2AndFlag(userName, friendName, 1);
            if (!_friendRelation1.isPresent()) {//说明我没有发送过请求
                FriendRelation friendRelation = new FriendRelation();
                friendRelation.setFlag(1);
                friendRelation.setWebUser1(webUserRepository.findOneByUserName(userName).get());
                friendRelation.setWebUser2(webUserRepository.findOneByUserName(friendName).get());
                friendRelationRepository.save(friendRelation);
            }
        }
        session.sendMessage(new TextMessage(aesKitService.Encrypt("apply one friend success!",SystemWebSocketHandler.symkey)));
        return true;
    }

    public void acceptOrRefuseApply(String[] payloadArray) {
        String altitude = payloadArray[0];
        Long applyId = Long.parseLong(payloadArray[1]);
        FriendRelation friendRelation = friendRelationRepository.findOne(applyId);//这个id不一定存在
        //不存在说明，对方已经通过请求了，这个数据被删除了，那么就不管
        if (friendRelation == null) {
            return;
        }
        WebUser webUser1 = friendRelation.getWebUser1();//发送请求的人
        WebUser webUser2 = friendRelation.getWebUser2();//接受请求的人，就是当前用户
        if (altitude.equals("accept")) {
            //首先删除所有请求
            List<FriendRelation> friendRelationList = friendRelationRepository.findAllApplies(webUser1.getUserName(), webUser2.getUserName());
            friendRelationRepository.delete(friendRelationList);
            //再把两个人变成朋友
            FriendRelation friendRelation1 = new FriendRelation();
            friendRelation1.setFlag(0);
            friendRelation1.setWebUser1(webUser1);
            friendRelation1.setWebUser2(webUser2);
            friendRelationRepository.save(friendRelation1);
        } else {
            //如果拒绝，那么就删除这一条数据就行
            friendRelationRepository.delete(applyId);
        }
    }

    public void applyPublicKey(String friendName, WebSocketSession webSocketSession) throws Exception {
        String pub = webUserRepository.findOneByUserName(friendName).get().getPublicKeyGetEncoded();
        webSocketSession.sendMessage(new TextMessage(aesKitService.Encrypt("applyPublicKey|" + pub,SystemWebSocketHandler.symkey)));
    }
}

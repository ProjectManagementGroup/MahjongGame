package org.mahjong.game.web.mvc.controller;

import org.mahjong.game.repository.WebUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.mahjong.game.domain.FriendRelation;
import org.mahjong.game.domain.WebUser;
import org.mahjong.game.repository.ChatMessageRepository;
import org.mahjong.game.repository.FriendRelationRepository;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


@Controller
@RequestMapping("/webUser")
public class WebUserController {
    private final Logger log = LoggerFactory.getLogger(WebUserController.class);

    @Inject
    private WebUserRepository webUserRepository;

    @Inject
    private FriendRelationRepository friendRelationRepository;

    @Inject
    private ChatMessageRepository chatMessageRepository;

    //登陆之后才确定了session，只有session就不变化了
    @RequestMapping(value = "/login")
    public String login(final Model model, HttpSession session) {
        //String time = DateTime.now().toString("yyyyMMddHHmmss");
        //session.setAttribute("user", time);
        //这里只返回页面，不分配session
        return "login";
    }

    //请求 我的好友列表页面
    @RequestMapping(value = "/friendList")
    public String friendList(@RequestParam(value = "userName") String userName, final Model model, HttpSession session) {
        //String time = DateTime.now().toString("yyyyMMddHHmmss");
        //session.setAttribute("user", time);
        Optional<WebUser> _user = webUserRepository.findOneByUserName(userName);
        if (!_user.isPresent()) {
            System.err.println("当前用户不存在！");
            return null;
        }
        List<String> friendNames = friendRelationRepository.findAllFriendsByUserName1(userName);
        friendNames.addAll(friendRelationRepository.findAllFriendsByUserName2(userName + ""));
        model.addAttribute("names", friendNames);
        return "friendList::content";
    }

    //请求 搜索好友页面
    @RequestMapping(value = "/addFriend")
    public String addFriend(@RequestParam(value = "userName") String userName, final Model model, HttpSession session) {
        return "meApplyFriend::content";
    }

    //请求 我收到的好友请求页面
    @RequestMapping(value = "/friendApply")
    public String friendApply(@RequestParam(value = "userName") String userName, final Model model, HttpSession session) {
        Optional<WebUser> _user = webUserRepository.findOneByUserName(userName);
        if (!_user.isPresent()) {
            System.err.println("当前用户不存在！");
            return null;
        }
        List<FriendRelation> friendRelations = friendRelationRepository.findAllAppliesByUserName(userName);
        model.addAttribute("friendRelations", friendRelations);
        return "friendApplyMe::content";
    }

    //请求 和朋友聊天
    @RequestMapping(value = "/chating")
    public String chating(@RequestParam(value = "userName") String userName, @RequestParam(value = "friendUserName") String friendUserName, final Model model, HttpSession session) {
        //查找聊天记录，并按照id排序
        //List<ChatMessage> chatMessages = chatMessageRepository.findAllByWebUser1AndWebUser2(userName, friendUserName);
        model.addAttribute("chatMessages", new LinkedList<>());
        model.addAttribute("friendUserName", friendUserName);
        return "chat::content";
    }
}

package org.mahjong.game.web.mvc.controller;

import org.mahjong.game.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.mahjong.game.domain.User;

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
    private UserRepository webUserRepository;

    //登陆之后才确定了session，只有session就不变化了
    @RequestMapping(value = "/login")
    public String login(final Model model, HttpSession session) {
        //String time = DateTime.now().toString("yyyyMMddHHmmss");
        //session.setAttribute("user", time);
        //这里只返回页面，不分配session
        return "login";
    }
}

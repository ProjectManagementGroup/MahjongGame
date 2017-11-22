package org.mahjong.game.web.mvc.controller;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;


@Controller
@RequestMapping("/home")
public class HomeController {
    private final Logger log = LoggerFactory.getLogger(HomeController.class);

    @RequestMapping(value = "/test")
    public String index(final Model model, HttpSession session) {
        String time = DateTime.now().toString("yyyyMMddHHmmss");
        session.setAttribute("login", time);
        return "test";
    }

    @RequestMapping(value = "/login" )
    public String login(final Model model, HttpSession session) {
        String time = DateTime.now().toString("yyyyMMddHHmmss");
        session.setAttribute("user", time);
        return "login";
    }
}

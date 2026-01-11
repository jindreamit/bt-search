package com.btsearch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web页面控制器
 */
@Controller
public class WebController {

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 登录/注册页面
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 用户管理页面
     */
    @GetMapping("/admin/users")
    public String users() {
        return "users";
    }
}

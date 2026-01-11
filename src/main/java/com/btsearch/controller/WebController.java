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
     * Token验证页面
     */
    @GetMapping("/token")
    public String token() {
        return "token";
    }

    /**
     * 种子详情页（移动端）
     */
    @GetMapping("/detail")
    public String detail() {
        return "detail";
    }
}

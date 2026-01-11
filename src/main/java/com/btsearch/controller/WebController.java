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
}

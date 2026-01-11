package com.btsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BitTorrent磁力搜索系统启动类
 * 支持多语言搜索（中英日韩俄等）
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BtSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BtSearchApplication.class, args);
    }
}

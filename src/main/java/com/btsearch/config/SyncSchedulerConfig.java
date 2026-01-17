package com.btsearch.config;

import com.btsearch.service.EsSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 同步任务调度配置
 * 只在 sync profile 下启用定时同步
 */
@Configuration
@Profile("sync")
@EnableScheduling
public class SyncSchedulerConfig {

    /**
     * 创建定时同步 Bean
     * 只在 sync profile 下才会被 Spring 容器管理
     */
    @Bean
    public SyncScheduler syncScheduler(EsSyncService esSyncService) {
        return new SyncScheduler(esSyncService);
    }

    /**
     * 定时同步调度器
     * 包装实际的同步逻辑
     */
    public static class SyncScheduler {
        private final EsSyncService esSyncService;

        public SyncScheduler(EsSyncService esSyncService) {
            this.esSyncService = esSyncService;
        }

        @Scheduled(fixedRate = 60000)  // 每分钟执行一次
        public void scheduledSync() {
            esSyncService.sync();
        }
    }
}

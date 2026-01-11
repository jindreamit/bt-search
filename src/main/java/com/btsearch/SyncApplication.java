package com.btsearch;

import com.btsearch.service.EsSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SyncApplication {

    private static final Logger log = LoggerFactory.getLogger(SyncApplication.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(SyncApplication.class)
            .profiles("sync")
            .web(null)  // Disable web server
            .run(args);
    }

    @Bean
    public CommandLineRunner syncRunner(EsSyncService esSyncService) {
        return args -> {
            log.info("=== ES Sync Application Started ===");
            try {
                esSyncService.sync();
                log.info("=== ES Sync Application Completed Successfully ===");
                System.exit(0);
            } catch (Exception e) {
                log.error("=== ES Sync Application Failed ===", e);
                System.exit(1);
            }
        };
    }
}

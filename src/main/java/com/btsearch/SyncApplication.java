package com.btsearch;

import com.btsearch.service.EsSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks
@Profile("sync")  // Only activate when sync profile is set
@ComponentScan(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {BtSearchApplication.class}  // Exclude main web application
    )
)
public class SyncApplication {

    private static final Logger log = LoggerFactory.getLogger(SyncApplication.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(SyncApplication.class)
            .profiles("sync")
            .web(null)  // Disable web server
            .run(args);
    }
}

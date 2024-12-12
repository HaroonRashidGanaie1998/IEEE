package com.example.Haroon.scheduler;

import com.example.Haroon.service.ApiService;
import com.example.Haroon.service.TokenGenerationService;
import org.apache.logging.log4j.spi.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@EnableScheduling
@Component
public class ExportDataToFile {
    Logger logger = LoggerFactory.getLogger(ExportDataToFile.class);
    @Autowired
    private TokenGenerationService tokenService;

    @Autowired
    private ApiService appService;

    @Scheduled(cron = "${schedule.cron}")
    public void runCronJob() {
        try {
            logger.info("Export Data start at :: "+ LocalDateTime.now());
            String token = tokenService.getToken();
            appService.fetchMembersInBatches(token);
            logger.info("Export Data completed at :: "+ LocalDateTime.now());
        }catch (Exception ex){
            ex.printStackTrace();
            logger.error("Exception was thrown while exporting the data to file "+ex.getMessage());
        }
        }
}

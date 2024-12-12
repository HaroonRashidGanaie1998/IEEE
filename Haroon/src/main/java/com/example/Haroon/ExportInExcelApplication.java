package com.example.Haroon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.Haroon.service.ApiService;
import com.example.Haroon.service.TokenGenerationService;
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.Haroon")
public class ExportInExcelApplication {
	
	@Autowired
	private TokenGenerationService tokenService;
	
	@Autowired
	private ApiService appService;
	
    public static void main(String[] args) {
        SpringApplication.run(ExportInExcelApplication.class, args);
    }
    
    
    @Scheduled(cron = "${schedule.cron}")
    public void runCronJob() {
    	String token = tokenService.getToken();
    	appService.fetchMembersInBatches(token);
    }
}


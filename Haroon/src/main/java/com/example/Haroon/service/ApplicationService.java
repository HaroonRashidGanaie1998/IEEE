package com.example.Haroon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.example.Haroon.model.ApplicationUsers;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApplicationService {

    @Autowired
    TokenGenerationService tokenGenerationService;

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestTemplate restTemplate;

    @Value("${api.member.application.url}")
    private String applicationUrl;

    @Value("${batch.size}")
    private int batchSize;

    @Value("${api.rate.limit.retry.max}")
    private int maxRetries;

    @Value("${api.rate.limit.retry.delay}")
    private long initialDelay;

    private int successfulCalls = 0;
    private int failedCalls = 0;


    public ApplicationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        logger.info("ApplicationService instantiated with RestTemplate.");
    }

    public List<ApplicationUsers> fetchApplicationDetailsForMember(String memberId, String token) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID cannot be null or empty.");
        }

        logger.info("Fetching application details for memberId: {}", memberId);

        String finalUrl = applicationUrl.replace("{memberId}", memberId);
        logger.debug("Application API URL after replacement: {}", finalUrl);

        List<ApplicationUsers> applicationUsersList = new ArrayList<>();
        int retries = 0;

        while (retries < maxRetries) {
            HttpHeaders headers = new HttpHeaders();
            headers.set(AUTHORIZATION_HEADER, BEARER_PREFIX + tokenGenerationService.getToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<List<ApplicationUsers>> responseEntity = restTemplate.exchange(
                        finalUrl,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<ApplicationUsers>>() {}
                );

                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    successfulCalls++;
                    List<ApplicationUsers> batch = responseEntity.getBody();
                    if (batch == null || batch.isEmpty()) {
                        logger.info("No more application data to fetch for memberId: {}", memberId);
                        break;
                    }

                    applicationUsersList.addAll(batch);
                    logger.info("Fetched {} application records so far for memberId: {}", applicationUsersList.size(), memberId);
                    if (batch.size() < batchSize) break;

                    retries = 0; 
                }

            } catch (HttpClientErrorException.Unauthorized ex) {
                failedCalls++;
                logger.warn("401 Unauthorized for memberId: {}. Refreshing token and retrying...", memberId);
                token = tokenGenerationService.getToken();  
                retries++;
                if (retries >= maxRetries) {
                    logger.error("Max retries reached for memberId: {} after refreshing token.", memberId);
                    throw new RuntimeException("Failed to fetch application details after multiple retries due to 401 Unauthorized", ex);
                }
            } catch (HttpClientErrorException.Forbidden ex) {
                failedCalls++;
                handleRateLimitError(memberId, retries++);
            } catch (Exception ex) {
                failedCalls++;
                logger.error("Unexpected error fetching data from API for memberId: {}", memberId, ex);
                throw new RuntimeException("Failed to fetch application details", ex);
            }

            try {
                Thread.sleep(initialDelay);  // Adding a delay between retries
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for the delay", ie);
            }
        }

        logger.info("Completed fetching application details for memberId: {}. Total records: {}", memberId, applicationUsersList.size());
        logger.info("Total successful API calls Application api: {}", successfulCalls);
        logger.info("Total failed API calls Application api: {}", failedCalls);

        return applicationUsersList;
    }


    private void handleRateLimitError(String memberId, int retries) {
        if (retries >= maxRetries) {
            logger.error("Max retries reached for memberId: {}, giving up.", memberId);
            throw new RuntimeException("Failed to fetch application details after multiple retries");
        }

        long backoffTime = (long) Math.pow(2, retries) * initialDelay;
        logger.warn("Rate limit exceeded for memberId: {}. Retrying in {} ms...", memberId, backoffTime);

        try {
            Thread.sleep(backoffTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for retry", ie);
        }
    }
}

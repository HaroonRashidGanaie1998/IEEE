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
import com.example.Haroon.model.PackageUsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PackageKeyService {

    @Autowired
    TokenGenerationService tokenGenerationService;

    private static final Logger logger = LoggerFactory.getLogger(PackageKeyService.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestTemplate restTemplate;

    @Value("${api.member.package.url}")
    private String packageUrl;

    @Value("${batch.size}")
    private int batchSize;

    @Value("${api.rate.limit.retry.max}")
    private int maxRetries;

    @Value("${api.rate.limit.retry.delay}")
    private long initialDelay;

    private int successfulCalls = 0;
    private int failedCalls = 0;

    public PackageKeyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        logger.info("PackageKeyService instantiated with RestTemplate.");
    }

    public List<PackageUsers> fetchMembersDataById(String token, String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID cannot be null or empty.");
        }

        logger.info("Starting to fetch package keys for memberId: {}", memberId);
        List<PackageUsers> packageUsersList = new ArrayList<>();
        int retries = 0;

        while (retries < maxRetries) {
            HttpHeaders headers = new HttpHeaders();
            headers.set(AUTHORIZATION_HEADER, BEARER_PREFIX + tokenGenerationService.getToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            Map<String, String> urlParams = new HashMap<>();
            urlParams.put("memberId", memberId);

            try {
                ResponseEntity<List<PackageUsers>> response = restTemplate.exchange(
                        packageUrl,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<PackageUsers>>() {},
                        urlParams
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    successfulCalls++;
                    List<PackageUsers> batch = response.getBody();
                    if (batch == null || batch.isEmpty()) {
                        logger.info("No more data to fetch for memberId: {}", memberId);
                        break;
                    }
                    packageUsersList.addAll(batch);
                    logger.info("Fetched {} records so far for memberId: {}", packageUsersList.size(), memberId);
                    if (batch.size() < batchSize) break;

                    retries = 0;  // Reset retries on success
                }

            } catch (HttpClientErrorException.Unauthorized ex) {
                failedCalls++;
                logger.warn("401 Unauthorized for memberId: {}. Refreshing token and retrying...", memberId);
                token = tokenGenerationService.getToken();  // Refresh token
                retries++;
                if (retries >= maxRetries) {
                    logger.error("Max retries reached for memberId: {} after refreshing token.", memberId);
                    throw new RuntimeException("Failed to fetch package details after multiple retries due to 401 Unauthorized", ex);
                }
            } catch (HttpClientErrorException.Forbidden ex) {
                failedCalls++;
                handleRateLimitError(memberId, retries++);
            } catch (Exception ex) {
                failedCalls++;
                logger.error("Unexpected error fetching data for memberId: {}", memberId, ex);
                throw new RuntimeException("Failed to fetch package details", ex);
            }

            try {
                Thread.sleep(initialDelay);  // Adding a delay between retries
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for the delay", ie);
            }
        }

        logger.info("Completed fetching package keys for memberId: {}. Total records: {}", memberId, packageUsersList.size());
        logger.info("Total successful API calls Package api: {}", successfulCalls);
        logger.info("Total failed API calls Package api: {}", failedCalls);

        return packageUsersList;
    }



    private void handleRateLimitError(String memberId, int retries) {
        if (retries >= maxRetries) {
            logger.error("Max retries reached for memberId: {}, giving up.", memberId);
            throw new RuntimeException("Failed to fetch package details after multiple retries");
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

package com.example.Haroon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.Haroon.model.PackageUsers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PackageKeyService {

    private static final Logger logger = LoggerFactory.getLogger(PackageKeyService.class);

    private final RestTemplate restTemplate;

    @Value("${api.member.package.url}") // URL template for fetching data
    private String packageUrl;

    @Value("${batch.Size}")
    private int batchSize;

    @Value("${api.qps.limit:5}") // Default QPS (queries per second) limit
    private int qpsLimit;

    private final ObjectMapper objectMapper;

    public PackageKeyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // To handle Java time objects
    }

    /**
     * Fetch package user data for a specific member ID with pagination and QPS control.
     *
     * @param token    Bearer token for API authentication.
     * @param memberId The member ID to fetch package keys for.
     * @return List of PackageUsers.
     */
    public List<PackageUsers> fetchMembersDataById(String token, String memberId) {
        logger.info("Starting to fetch package keys for memberId: {}", memberId);

        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Prepare the base URL with path variable placeholder
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("memberId", memberId);

        List<PackageUsers> packageUsersList = new ArrayList<>();
        boolean hasMoreData = true;
        int offset = 0;

        while (hasMoreData) {
            try {
                // Add rate-limiting delay
                TimeUnit.MILLISECONDS.sleep(1000 / qpsLimit);

                // Construct paginated URL with offset
                String paginatedUrl = packageUrl + "?offset=" + offset + "&limit=" + batchSize;

                logger.debug("Fetching data from URL: {}", paginatedUrl);

                // Execute API call with pagination
                ResponseEntity<List<PackageUsers>> response = restTemplate.exchange(
                        paginatedUrl,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<PackageUsers>>() {
                        },
                        urlParams
                );

                List<PackageUsers> batch = response.getBody();

                // Check if there are results in the current batch
                if (batch == null || batch.isEmpty()) {
                    logger.info("No more data to fetch for memberId: {}", memberId);
                    hasMoreData = false; // Stop pagination
                } else {
                    packageUsersList.addAll(batch);
                    logger.info("Fetched {} records so far for memberId: {}", packageUsersList.size(), memberId);

                    // Move to the next page by increasing the offset
                    offset += batchSize;
                }

            } catch (HttpClientErrorException.Forbidden e) {
                // Handle rate limit exception (HTTP 403)
                logger.warn("Rate limit exceeded for memberId: {}. Waiting for retry...", memberId);

                try {
                    TimeUnit.SECONDS.sleep(2); // Static retry delay for simplicity
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Thread interrupted during retry delay", ie);
                    throw new RuntimeException("Retry interrupted", ie);
                }

            } catch (Exception e) {
                logger.error("Error while fetching package keys for memberId: {}: {}", memberId, e.getMessage(), e);
                hasMoreData = false; // Stop on unrecoverable error
            }
        }

        logger.info("Completed fetching package keys for memberId: {}. Total records: {}", memberId, packageUsersList.size());
        return packageUsersList;
    }
}

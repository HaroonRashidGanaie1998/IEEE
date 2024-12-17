package com.example.Haroon.service;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.Haroon.model.ApplicationUsers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    private final RestTemplate restTemplate;

    @Value("${api.member.application.url}")
    private String applicationUrl;

    @Value("${batch.Size}")
    private int batchSize;

    @Value("${api.rate.limit.retry.max}")  // Max number of retries
    private int maxRetries;

    @Value("${api.rate.limit.retry.delay}")  // Initial delay in ms
    private long initialDelay;

    public ApplicationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        logger.info("ApplicationService instantiated with RestTemplate.");
    }

    // Fetch Application details for a specific member ID with retry and backoff
    public List<ApplicationUsers> fetchApplicationDetailsForMember(String memberId, String token) {
        logger.info("Fetching Application details for memberId: {}", memberId);

        // Construct the final URL by replacing the placeholder with the memberId
        String finalUrl = applicationUrl.replace("{memberId}", memberId);
        logger.debug("Application API URL after replacement: {}", finalUrl);

        // Set up the Authorization header with the Bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        logger.debug("Authorization token set.");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<ApplicationUsers> applicationUsersList = new ArrayList<>();
        int offset = 0;
        int retries = 0;

        // Paginate through the results with retry logic
        while (true) {
            String paginatedUrl = finalUrl + "?offset=" + offset;  // Add offset for pagination
            logger.debug("Fetching from paginated URL: {}", paginatedUrl);

            try {
                ResponseEntity<List<ApplicationUsers>> responseEntity = restTemplate.exchange(
                        paginatedUrl,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<ApplicationUsers>>() {}
                );

                List<ApplicationUsers> batch = responseEntity.getBody();

                // If there are no records, stop pagination and handle no data case
                if (batch == null || batch.isEmpty()) {
                    logger.info("No more Application data to fetch for memberId: {}", memberId);
                    break;
                }

                applicationUsersList.addAll(batch);
                logger.info("Fetched {} Application records so far for memberId: {}", applicationUsersList.size(), memberId);

                // Update offset for the next request
                offset += batch.size();

                // If the batch size is less than the limit, we have reached the end of data
                if (batch.size() < batchSize) break;

                retries = 0; // Reset retries on successful fetch

            } catch (HttpClientErrorException.Forbidden ex) {
                // Check if the error is due to rate-limiting (HTTP 403)
                if (ex.getMessage().contains("Developer Over Qps")) {
                    if (retries >= maxRetries) {
                        logger.error("Max retries reached for memberId: {}, giving up.", memberId);
                        throw new RuntimeException("Failed to fetch application details after multiple retries", ex);
                    }

                    retries++;
                    long backoffTime = (long) Math.pow(2, retries) * initialDelay;  // Exponential backoff
                    logger.warn("Rate limit exceeded for memberId: {}. Retrying in {} ms...", memberId, backoffTime);

                    try {
                        Thread.sleep(backoffTime);  // Sleep before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for retry", ie);
                    }

                } else {
                    logger.error("Error fetching data from API for memberId: {}", memberId, ex);
                    throw new RuntimeException("Failed to fetch application details", ex);
                }
            } catch (Exception ex) {
                logger.error("Unexpected error fetching data from API for memberId: {}", memberId, ex);
                throw new RuntimeException("Failed to fetch application details", ex);
            }
        }

        logger.info("Total Application records fetched: {} for memberId: {}", applicationUsersList.size(), memberId);
        return applicationUsersList;
    }

    // Other methods (generateExcelForApplications, saveExcelToFile, etc.) remain unchanged...

    // Helper method to return "NA" if value is null
    private String getOrDefault(String value) {
        String result = (value != null) ? value : "NA";
        logger.debug("Returning value for null check: {}", result);
        return result;
    }
}

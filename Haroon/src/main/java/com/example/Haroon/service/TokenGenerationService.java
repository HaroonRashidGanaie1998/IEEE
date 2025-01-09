package com.example.Haroon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;

@Service
public class TokenGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(TokenGenerationService.class);

    @Value("${api.token.url}")
    private String tokenUrl;

    @Value("${api.clientId}")
    private String clientId;

    @Value("${api.clientSecret}")
    private String clientSecret;

    @Value("${api.username}")
    private String username;

    @Value("${api.password}")
    private String password;

    @Value("${api.scope}")
    private String scope;

    @Value("${default.token.expiration}") 
    private long defaultTokenExpiration;

    private  String accessToken;
    private Instant tokenExpiration;

    private final RestTemplate restTemplate;

    public TokenGenerationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getToken() {
        logger.info("Checking if token is valid or needs regeneration.");
        if (accessToken == null || isTokenExpired()) {
            logger.info("Token is either null or expired. Generating a new token.");
            generateToken();
        } else {
            logger.info("Token is still valid.");
        }
        return accessToken;
    }

    private void generateToken() {
        logger.info("Initiating token generation process.");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username);
            body.add("password", password);
            body.add("scope", scope);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                String.class
            );

            // Handle response
            if (response.getStatusCode().is2xxSuccessful()) {
                accessToken = extractTokenFromResponse(response.getBody());
                tokenExpiration = Instant.now().plusSeconds(extractTokenExpiration(response.getBody()));

                logger.info("Token generated successfully. Expires at: {}", tokenExpiration);
            } else {
                logger.error("Failed to generate token. Response: {}", response.getBody());
                accessToken = null; // Reset the token in case of failure
            }
        } catch (Exception e) {
            logger.error("Error occurred while generating token: {}", e.getMessage(), e);
            accessToken = null; // Ensure accessToken is reset in case of failure
        }
    }

    private boolean isTokenExpired() {
        return tokenExpiration == null || Instant.now().isAfter(tokenExpiration);
    }

    private String extractTokenFromResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (rootNode.has("access_token")) {
                return rootNode.get("access_token").asText();
            }
        } catch (Exception e) {
            logger.error("Error extracting token: {}", e.getMessage(), e);
        }
        return null;
    }

    private long extractTokenExpiration(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (rootNode.has("expires_in")) {
                return rootNode.get("expires_in").asLong();
            }
        } catch (Exception e) {
            logger.error("Error extracting token expiration: {}", e.getMessage(), e);
        }
        return defaultTokenExpiration; 
    }

    // Optional: This method can be scheduled to periodically refresh the token.
//    @Scheduled(fixedRate = 1800000) // Every 30 minutes (adjust according to your needs)
//    public void refreshTokenPeriodically() {
//        if (isTokenExpired()) {
//            logger.info("Token expired, refreshing...");
//            generateToken();
//        } else {
//            logger.info("Token is still valid. No refresh needed.");
//        }
//    }
}
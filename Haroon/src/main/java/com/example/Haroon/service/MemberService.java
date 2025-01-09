package com.example.Haroon.service;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.Haroon.model.Members;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemberService {
	@Autowired
	TokenGenerationService tokenGenerationService;

    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);

    private final RestTemplate restTemplate;

    @Value("${api.members.url}")
    private String membersUrl;

    @Value("${batch.size}")
    private int batchSize;

    public MemberService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Members> fetchMembersInBatches(String token) {
        logger.info("Fetching member data in batches with batch size: {}", batchSize);
        logger.debug("Using token: {}", token); 

        List<Members> allMembers = new ArrayList<>();
        int offset = 0;

        while (true) {
            logger.debug("Fetching batch with offset: {}, limit: {}", offset, batchSize);
            List<Members> batch = fetchBatchOfMembers(token, offset, batchSize);
            if (batch.isEmpty()) {
                logger.info("No more data to fetch. Stopping batch processing.");
                break;
            }
            allMembers.addAll(batch);
            logger.info("Fetched {} members in the current batch, total fetched so far: {}", batch.size(), allMembers.size());

           
            offset += 1;

            
            if (batch.size() < batchSize) {
                logger.info("Received a batch smaller than the requested size. Stopping batch processing.");
                break;
            }
        }
        logger.info("Total members fetched: {}", allMembers.size());
        byte[] excelFile = generateLargeExcelFile(allMembers);

        return allMembers;
    }

    private List<Members> fetchBatchOfMembers(String token, int offset, int limit) {
        logger.debug("Fetching members from URL: {} with offset: {} and limit: {}", membersUrl, offset, limit);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokenGenerationService.getToken());

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String paginatedUrl = membersUrl + "?offset=" + offset + "&limit=" + limit;

        logger.debug("Making API call to: {}", paginatedUrl);
        ResponseEntity<List<Members>> responseEntity = restTemplate.exchange(
            paginatedUrl,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<Members>>() {}
        );

        logger.info("API response status: {}", responseEntity.getStatusCode());
        String totalCountHeader = responseEntity.getHeaders().getFirst("X-Total-Count");
        if (totalCountHeader != null) {
            logger.info("X-Total-Count header value: {}", totalCountHeader);
        }

        List<Members> members = responseEntity.getBody();
        if (members != null && !members.isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonResponse = objectMapper.writeValueAsString(members);
                logger.info("Member API response JSON: {}", jsonResponse);
            } catch (Exception e) {
                logger.error("Error serializing API response to JSON: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("API returned an empty or null response.");
            return new ArrayList<>();
        }

        logger.info("API response body size: {}", members.size());
        return members;
    }

    
    public byte[] generateLargeExcelFile(List<Members> members) {
        logger.info("Starting batch processing for {} members.", members.size());
        members.sort((m1, m2) -> {
            String date1 = formatDate(m1.getCreated());
            String date2 = formatDate(m2.getCreated());
            if (date1.equals("N/A")) {
                return 1;
            }
            if (date2.equals("N/A")) {
                return -1;
            }
            return date2.compareTo(date1);
        });

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(batchSize);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Members Data");
            int rowIndex = 1;
            for (int start = 0; start < members.size(); start += batchSize) {
                int end = Math.min(start + batchSize - 1, members.size() - 1); 
                List<Members> batch = members.subList(start, end + 1); 
                rowIndex = appendBatchToSheet(sheet, batch, rowIndex);
                logger.info("Processed batch from {} to {}", start + 1, end + 1); 
            
               
            }

            workbook.write(outputStream);
           

            return outputStream.toByteArray();

        } catch (IOException e) {
            logger.error("Error occurred while generating Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    private String formatDate(String date) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return date != null ? date : "N/A";
        }
    }

    private int appendBatchToSheet(Sheet sheet, List<Members> batch, int startRowIndex) {
        return startRowIndex;
    }

    public void saveExcelFileToLocal(byte[] excelData, String filePath) {
        Logger logger = LoggerFactory.getLogger(ApplicationService.class);

        // Path to save the Excel file
        String savePath = "C:\\Excelfile\\";
        String fullFilePath = savePath + filePath;

        try (FileOutputStream fos = new FileOutputStream(new File(fullFilePath))) {
            fos.write(excelData);
            logger.info("Excel file saved successfully at: {}", fullFilePath);
        } catch (IOException e) {
            logger.error("Failed to save Excel file at {}: {}", fullFilePath, e.getMessage(), e);
        }
}
}
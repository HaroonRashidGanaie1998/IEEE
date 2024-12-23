package com.example.Haroon.service;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        logger.debug("Using token: {}", token); // Log the token being used

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

            offset += batch.size();

            if (batch.size() < batchSize) {
                logger.info("Received a batch smaller than the requested size. Stopping batch processing.");
                break;
            }
        }
        logger.info("Total members fetched: {}", allMembers.size());
        byte[]excelFile = generateLargeExcelFile(allMembers);

        return allMembers;
    }

    private List<Members> fetchBatchOfMembers(String token, int offset, int limit) {
        logger.debug("Fetching members from URL: {} with offset: {} and limit: {}", membersUrl, offset, limit);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String paginatedUrl = membersUrl + "?offset=" + offset;
        if (limit != 1000) {
            paginatedUrl = paginatedUrl.concat("&limit=" + limit);
        }

        logger.debug("Making API call to: {}", paginatedUrl);
        ResponseEntity<List<Members>> responseEntity = restTemplate.exchange(
                paginatedUrl,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Members>>() {}
        );

        logger.info("API response status: {}", responseEntity.getStatusCode());
        
        List<Members> members = responseEntity.getBody();
        if (members != null) {
            try {
            	 // Log the JSON response in a single line
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
           // createHeaderRow(sheet);
            int rowIndex = 1;
            for (int i = 0; i < members.size(); i += batchSize) {
                int end = Math.min(i + batchSize, members.size());
                List<Members> batch = members.subList(i, end);
                rowIndex = appendBatchToSheet(sheet, batch, rowIndex);
                logger.info("Processed batch from {} to {}", i, end);
            }

            workbook.write(outputStream);
            logger.info("Excel file generated successfully with {} rows.", rowIndex - 1);

            return outputStream.toByteArray();

        } catch (IOException e) {
            logger.error("Error occurred while generating Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

//    private void createHeaderRow(Sheet sheet) {
//        Row headerRow = sheet.createRow(0);
//       
//        String[] headers = {
//            "", "Date", "Email", "Customer Name", "Institution/Organization", "Country of Origin",
//            "Use Case", "API Key Status", "Type of Institution", "User Name", "API Key"
//        };
//        for (int i = 0; i < headers.length; i++) {
//            headerRow.createCell(i).setCellValue(headers[i]);
//        }
//    }

    private String formatDate(String date) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return date != null ? date : "N/A";
        }
    }

    private int appendBatchToSheet(Sheet sheet, List<Members> batch, int startRowIndex) {
//        // Set the header row if it's the first row
//        if (startRowIndex == 1) {
//            Row headerRow = sheet.createRow(0);  // Row 0 is the header row
//            headerRow.createCell(0).setCellValue("");  // Empty cell for the first column
//            headerRow.createCell(1).setCellValue("Date");
//            headerRow.createCell(2).setCellValue("Email");
//            headerRow.createCell(3).setCellValue("Customer Name");
//            headerRow.createCell(4).setCellValue("Institution/Organization");
//            headerRow.createCell(5).setCellValue("Country of Origin");
//           // headerRow.createCell(6).setCellValue("Use Case");
//          //  headerRow.createCell(7).setCellValue("API Key Status");
//            headerRow.createCell(8).setCellValue("Type of Institution");
//            headerRow.createCell(9).setCellValue("User Name");
//           // headerRow.createCell(10).setCellValue("API Key");
//        }
//
//        // Iterate through the batch and add rows
//        for (Members member : batch) {
//            String customerName = getOrDefault(member.getFirstName()) + " " + getOrDefault(member.getLastName());
//            String date = formatDate(getOrDefault(member.getCreated()));
//            String institutionOrOrganization = getOrDefault(member.getCompany());
//            String countryOfOrigin = getOrDefault(member.getCountryCode());
////            String useCase = getOrDefault(member.getDescription());
////            String apiKeyStatus = getOrDefault(member.getApiKeyStatus());
//            String typeOfInstitution = "Not Available"; // Placeholder
//            String username = getOrDefault(member.getUsername());
//            //String apiKey = getOrDefault(member.getApiKey());
//
//            // Create the row and add data
//            Row row = sheet.createRow(startRowIndex++);
//            row.createCell(1).setCellValue(date);
//            row.createCell(2).setCellValue(getOrDefault(member.getEmail()));
//            row.createCell(3).setCellValue(customerName);
//            row.createCell(4).setCellValue(institutionOrOrganization);
//            row.createCell(5).setCellValue(countryOfOrigin);
////            row.createCell(6).setCellValue(useCase);
////            row.createCell(7).setCellValue(apiKeyStatus);
//            row.createCell(8).setCellValue(typeOfInstitution);
//            row.createCell(9).setCellValue(username);
//          //  row.createCell(10).setCellValue(apiKey);
//        }

        return startRowIndex;
    }


    public void saveExcelFileToLocal(byte[] excelData, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
            fos.write(excelData);
            logger.info("Excel file saved successfully at: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to save Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save Excel file", e);
        }
    }
}
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
import org.springframework.web.client.RestTemplate;

import com.example.Haroon.model.Members;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    private final RestTemplate restTemplate;

    @Value("${api.token.url}")
    private String tokenUrl;

    @Value("${api.members.url}")
    private String membersUrl;

    @Value("${batch.size}")
    private int batchSize;

    public ApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Members> fetchMembersInBatches(String token) {
        logger.info("Fetching member data in batches with batch size: {}", batchSize);

        List<Members> allMembers = new ArrayList<>();
        int offset = 0;

        while (true) {
            List<Members> batch = fetchBatchOfMembers(token, offset, batchSize);
            if (batch.isEmpty()) {
                logger.info("No more data to fetch. Stopping batch processing.");
                break;
            }
            allMembers.addAll(batch);
            logger.info("Fetched {} members in the current batch, total fetched so far: {}", batch.size(), allMembers.size());

            offset += batch.size();

            if (batch.size() >batchSize) {
                logger.info("Received a batch smaller than the requested size. Stopping batch processing.");
                break;
            }
        }

        logger.info("Total members fetched: {}", allMembers.size());

       
        byte[] excelFile = generateLargeExcelFile(allMembers);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filePath = "/home/Data/MembersData_" + timestamp + ".xlsx";
        saveExcelFileToLocal(excelFile, filePath);

        return allMembers;
    }

    private List<Members> fetchBatchOfMembers(String token, int offset, int limit) {
        logger.info("Fetching members from URL: {} with offset: {} and limit: {}", membersUrl, offset, limit);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String paginatedUrl = membersUrl + "?offset=" + offset;
        if (limit != 0) {
            paginatedUrl = paginatedUrl.concat("&limit=" + limit);
        }

        ResponseEntity<List<Members>> responseEntity = restTemplate.exchange(
                paginatedUrl,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Members>>() {}
        );

        logger.info("API response status: {}", responseEntity.getStatusCode());
        logger.info("API response body size: {}", responseEntity.getBody() != null ? responseEntity.getBody().size() : 0);

        return responseEntity.getBody() != null ? responseEntity.getBody() : new ArrayList<>();
    }

    public byte[] generateLargeExcelFile(List<Members> members) {
        logger.info("Starting batch processing for {} members.", members.size());

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(batchSize);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Members Data");
            createHeaderRow(sheet);

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

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "id", "created", "updated", "username", "email", "countryCode",
            "firstName", "lastName", "lastLogin", "DisplayName"
        };
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
    }

    private int appendBatchToSheet(Sheet sheet, List<Members> batch, int startRowIndex) {
        for (Members member : batch) {
            Row row = sheet.createRow(startRowIndex++);
            row.createCell(0).setCellValue(getOrDefault(member.getId()));
            row.createCell(1).setCellValue(getOrDefault(member.getCreated()));
            row.createCell(2).setCellValue(getOrDefault(member.getUpdated()));
            row.createCell(3).setCellValue(getOrDefault(member.getUsername()));
            row.createCell(4).setCellValue(getOrDefault(member.getEmail()));
            row.createCell(5).setCellValue(getOrDefault(member.getCountryCode()));
            row.createCell(6).setCellValue(getOrDefault(member.getFirstName()));
            row.createCell(7).setCellValue(getOrDefault(member.getLastName()));
            row.createCell(8).setCellValue(getOrDefault(member.getLastLogin()));
            row.createCell(9).setCellValue(getOrDefault(member.getDisplayName()));
        }
        return startRowIndex;
    }

    private String getOrDefault(String value) {
        return value != null ? value : "NA";
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

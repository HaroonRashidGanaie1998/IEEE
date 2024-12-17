package com.example.Haroon.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Haroon.model.ApplicationUsers;
import com.example.Haroon.model.Members;
import com.example.Haroon.model.PackageUsers;
import com.example.Haroon.service.ApplicationService;
import com.example.Haroon.service.MemberService;
import com.example.Haroon.service.PackageKeyService;
import com.example.Haroon.service.TokenGenerationService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class ExcelExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportController.class);

    @Autowired
    private TokenGenerationService tokenGenerationService;
    
    @Autowired
    private MemberService memberService;
    
    @Autowired
    private ApplicationService applicationService;
    
    @Autowired
    private PackageKeyService packageKeyService;

    @GetMapping("/downloadExcel")
    public void downloadExcel(HttpServletResponse response,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        try {
            logger.info("Starting the Excel download process");

            // Get token for authorization
            String token = tokenGenerationService.getToken();
            logger.debug("Generated token: {}", token);

            // Fetch members with pagination
            List<Members> membersList = memberService.fetchMembersInBatches(token);
            logger.info("Fetched {} members from MemberService", membersList.size());

            // Create Excel file and write data
            try (SXSSFWorkbook workbook = new SXSSFWorkbook();
                 OutputStream outputStream = response.getOutputStream()) {
                Sheet sheet = workbook.createSheet("Combined Data");
                int rowIndex = 1; // Start from row 1 for data insertion

                // Create headers for the Excel file
                createHeaderRow(sheet);

                // Iterate over members and fetch associated application and package data
                for (Members member : membersList) {
                    String memberId = member.getId();

                    // Create the row for the member
                    Row row = sheet.createRow(rowIndex++);
                    String customerName = getOrDefault(member.getFirstName()) + " " + getOrDefault(member.getLastName());
                    String date = formatDate(getOrDefault(member.getCreated()));
                    String institutionOrOrganization = getOrDefault(member.getCompany());
                    String countryOfOrigin = getOrDefault(member.getCountryCode());
                    String typeOfInstitution = "Not Available"; // Placeholder
                    String username = getOrDefault(member.getUsername());

                    // Insert member basic information into the row
                    row.createCell(1).setCellValue(date);
                    row.createCell(2).setCellValue(getOrDefault(member.getEmail()));
                    row.createCell(3).setCellValue(customerName);
                    row.createCell(4).setCellValue(institutionOrOrganization);
                    row.createCell(5).setCellValue(countryOfOrigin);
                    row.createCell(8).setCellValue(typeOfInstitution);
                    row.createCell(9).setCellValue(username);

                    // Fetch Application Users and insert in the same row for this user
                    List<ApplicationUsers> applicationData = applicationService.fetchApplicationDetailsForMember(memberId, token);
                    if (applicationData != null && !applicationData.isEmpty()) {
                        ApplicationUsers applicationUser = applicationData.get(0);  // Assuming one use case per member
                        String useCase = getOrDefault(applicationUser.getDescription());
                        row.createCell(6).setCellValue(useCase);  // Insert use case in the same row
                    }

                    // Fetch Package Users and insert in the same row for this user
                    List<PackageUsers> packageData = packageKeyService.fetchMembersDataById(token, memberId);
                    if (packageData != null && !packageData.isEmpty()) {
                        PackageUsers packageUser = packageData.get(0);  // Assuming one API key per member
                        String APIKey = getOrDefault(packageUser.getApikey());
                        String APIKeyStatus = getOrDefault(packageUser.getStatus());
                        row.createCell(7).setCellValue(APIKeyStatus);  // Insert API key status
                        row.createCell(10).setCellValue(APIKey);  // Insert API key
                    }
                }

                // Set response headers for file download
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment; filename=CombinedData.xlsx");

                workbook.write(outputStream);
                logger.info("Excel file download completed successfully");

            } catch (IOException e) {
                logger.error("Error during Excel file generation: ", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error during Excel file generation.");
            }

        } catch (Exception e) {
            logger.error("Error during Excel download: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Error during Excel file generation.");
            } catch (IOException ioException) {
                logger.error("Error writing response: ", ioException);
            }
        }
    }

    // Create Excel header row
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);  // Header row
        headerRow.createCell(1).setCellValue("Date");
        headerRow.createCell(2).setCellValue("Email");
        headerRow.createCell(3).setCellValue("Customer Name");
        headerRow.createCell(4).setCellValue("Institution/Organization");
        headerRow.createCell(5).setCellValue("Country of Origin");
        headerRow.createCell(6).setCellValue("Use Case");
        headerRow.createCell(7).setCellValue("API Key Status");
        headerRow.createCell(8).setCellValue("Type of Institution");
        headerRow.createCell(9).setCellValue("User Name");
        headerRow.createCell(10).setCellValue("API Key");
    }

    // Utility methods
    private String getOrDefault(String value) {
        return value != null ? value : "N/A";
    }

    private String formatDate(String date) {
        return (date != null) ? date : "N/A";
    }
}

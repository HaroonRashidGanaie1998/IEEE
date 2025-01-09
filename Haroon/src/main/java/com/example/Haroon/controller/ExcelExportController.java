package com.example.Haroon.controller;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;


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

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String EXCEL_FILE_NAME = "IEEEMasheryUsers.xlsx";

    @GetMapping("/downloadExcel")
    public void downloadExcel(HttpServletResponse response,
                              @RequestParam(required = false) List<String> memberIds,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "100") int pageSize) {
        logger.info("Starting the Excel download process");

        try (SXSSFWorkbook workbook = new SXSSFWorkbook();
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            // Fetch data and populate workbook
            String token = tokenGenerationService.getToken();
            List<Members> membersList = fetchMembers(token, memberIds);
            logger.info("Fetched {} members from MemberService", membersList.size());

            Sheet sheet = workbook.createSheet("Combined Data");
            createHeaderRow(sheet);

            membersList.forEach(member -> populateMemberData(sheet, member, token));

            // Write workbook to buffer
            workbook.write(byteArrayOutputStream);

            // Write buffer to response
            response.setContentType(EXCEL_CONTENT_TYPE);
            response.setHeader("Content-Disposition", "attachment; filename=" + EXCEL_FILE_NAME);
            response.getOutputStream().write(byteArrayOutputStream.toByteArray());

            logger.info("Excel file download completed successfully");

        } catch (Exception e) {
            logger.error("Error during Excel download", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Error during Excel download.");
            } catch (IOException ioException) {
                logger.error("Error writing response: ", ioException);
            }
        }
    }


    private List<Members> fetchMembers(String token, List<String> memberIds) {
        if (memberIds != null && !memberIds.isEmpty()) {
            logger.info("Fetching members for provided member IDs");
            return memberService.fetchMembersInBatches(token);
        } else {
            logger.info("Fetching members in batches");
            return memberService.fetchMembersInBatches(token);
        }
    }

    private void populateMemberData(Sheet sheet, Members member, String token) {
        String memberId = member.getId();
        Row row = sheet.createRow(sheet.getPhysicalNumberOfRows() + 1);

       
        insertMemberBasicData(row, member);

      //  Fetch Application Users and insert data
        List<ApplicationUsers> applicationData = applicationService.fetchApplicationDetailsForMember(memberId, token);
        insertApplicationData(row, applicationData, memberId);

        // Fetch Package Users and insert data
        List<PackageUsers> packageData = packageKeyService.fetchMembersDataById(token, memberId);
        insertPackageData(row, packageData ,memberId);
    }

    private void insertMemberBasicData(Row row, Members member) {
        String customerName = getOrDefault(member.getFirstName()) + " " + getOrDefault(member.getLastName());
        String date = formatDate(getOrDefault(member.getCreated()));
        String institutionOrOrganization = getOrDefault(member.getCompany());
        String countryOfOrigin = getOrDefault(member.getCountryCode(), member.getRegistrationIpaddr());
        String username = getOrDefault(member.getUsername());

        row.createCell(1).setCellValue(date);
        row.createCell(2).setCellValue(getOrDefault(member.getEmail()));
        row.createCell(3).setCellValue(customerName);
        row.createCell(4).setCellValue(institutionOrOrganization);
        row.createCell(5).setCellValue(countryOfOrigin);
        row.createCell(9).setCellValue(username);
    }

    private void insertApplicationData(Row row, List<ApplicationUsers> applicationData, String memberId) {
        if (applicationData != null && !applicationData.isEmpty()) {
            ApplicationUsers applicationUser = applicationData.get(0);
            String organizationType = applicationUser.getOrganization_type();
            String typeOfInstitution = parseOrganizationType(organizationType);

            String useCase = getOrDefault(applicationUser.getDescription());
            row.createCell(6).setCellValue(useCase);
            row.createCell(8).setCellValue(typeOfInstitution);
        } else {
            logger.warn("No application data found for memberId: {}", memberId);
        }
    }

    private void insertPackageData(Row row, List<PackageUsers> packageData ,String memberId ) {
        if (packageData != null && !packageData.isEmpty()) {
            PackageUsers packageUser = packageData.get(0);
            String APIKey = getOrDefault(packageUser.getApikey());
            String APIKeyStatus = getOrDefault(packageUser.getStatus());
            row.createCell(7).setCellValue(APIKeyStatus);
            row.createCell(10).setCellValue(APIKey);
        }else {
        	 logger.warn("No package data found for memberId: {}", memberId);
        }
    }

    private String parseOrganizationType(String organizationType) {
        if (organizationType != null && !organizationType.trim().isEmpty()) {
            try {
                int organizationTypeInt = Integer.parseInt(organizationType.trim());
                return getTypeOfInstitution(organizationTypeInt);
            } catch (NumberFormatException e) {
                logger.warn("Invalid organization type format: {}", organizationType);
                return "Invalid format";
            }
        }
        return "N/A";
    }

    private String getTypeOfInstitution(int organizationType) {
        switch (organizationType) {
            case 1: return "Academic";
            case 2: return "Corporate";
            case 3: return "Government";
            default: return "N/A";
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
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

    private String getOrDefault(String value) {
        return value != null ? value : "N/A";
    }

    private String getOrDefault(String value, String fallbackValue) {
        return value != null && !value.isEmpty() ? value : fallbackValue != null ? fallbackValue : "N/A";
    }

    private String formatDate(String date) {
        try {
            return Instant.parse(date)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (Exception e) {
            return date != null ? date : "N/A";
        }
    }
}

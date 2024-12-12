package com.example.Haroon.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Haroon.model.Members;
import com.example.Haroon.service.ApiService;
import com.example.Haroon.service.TokenGenerationService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class ExcelExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportController.class);

    @Autowired
    ApiService apiService;
    
    @Autowired
    TokenGenerationService tokenGenerationService;

    @GetMapping("/downloadExcel")
    public void downloadExcel(HttpServletResponse response) {
        try {
            String token = tokenGenerationService.getToken();
            List<Members> members = apiService.fetchMembersInBatches(token);
            byte[] excelFile = apiService.generateLargeExcelFile(members);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=MembersData.xlsx");
            response.getOutputStream().write(excelFile);
            response.flushBuffer();
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

//    @GetMapping("/downloadExcelZip")
//    public void downloadExcelZip(HttpServletResponse response) {
//        try {
//            String token = tokenGenerationService.getToken();
//            List<Members> members = apiService.fetchAllMembers(token);
//
//            // Generate ZIP file
//            byte[] zipData = apiService.generateBatchExcelAndZip(members);
//
//            // Configure response headers for ZIP file
//            response.setContentType("application/zip");
//            response.setHeader("Content-Disposition", "attachment; filename=MembersData.zip");
//
//            // Write ZIP data to response
//            response.getOutputStream().write(zipData);
//            response.getOutputStream().flush();
//        } catch (Exception e) {
//            logger.error("Error during ZIP download: ", e);
//        }
//    }

    }
    

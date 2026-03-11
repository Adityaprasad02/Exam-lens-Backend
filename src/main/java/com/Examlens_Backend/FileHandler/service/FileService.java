package com.Examlens_Backend.FileHandler.service;

import com.Examlens_Backend.FileHandler.Models.ResponseModel;
import com.Examlens_Backend.FileHandler.Models.TopicDetails;
import com.Examlens_Backend.FileHandler.exceptions.HandleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Value("${py.url}")
    private String url;

    @Value("${ocr.timeout.seconds:60}")
    private int ocrTimeoutSeconds;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("fileProcessingExecutor")
    private Executor executor;

    @Autowired
    private AIConnection conn ;

    private final ObjectMapper mapper = new ObjectMapper();


    public List<Map<String, Object>> handleUpload(MultipartFile[] files, List<TopicDetails>syllabus) {
        long startTime = Instant.now().toEpochMilli();
        log.info("=== UPLOAD START ===");
        log.info("Processing {} files with {} topics", files.length, syllabus.size());

        if (files == null || syllabus == null) {
            throw new HandleException("Required field is missing ! ! ");
        }
        if (syllabus.isEmpty()) {
            throw new HandleException("Syllabus is required");
        }

        // Validate all files first
        for (MultipartFile file : files) {
            validatePdf(file);
        }

        // Process files in parallel using CompletableFuture
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        for (MultipartFile file : files) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(
                    () -> processSingleFile(file, syllabus),
                    executor
            );
            futures.add(future);
        }

        // Wait for all to complete
        List<Map<String, Object>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        long duration = Instant.now().toEpochMilli() - startTime;
        log.info("=== UPLOAD COMPLETE ===");
        log.info("Total time: {} ms for {} files", duration, files.length);
        log.info("Average time per file: {} ms", duration / files.length);

        return results;
    }

    private Map<String, Object> processSingleFile(MultipartFile file , List<TopicDetails>syllabus) {
        String fileName = file.getOriginalFilename();
        long fileStartTime = Instant.now().toEpochMilli();
        log.debug("Processing file: {}", fileName);

        File tempPdf = null;
        try {
            // Step 1: Disk I/O (save)
            long t1 = Instant.now().toEpochMilli();
            double size = getSize(file);
            tempPdf = saveTempPdf(file);
            long diskTime = Instant.now().toEpochMilli() - t1;
            log.debug("Disk I/O time for {}: {} ms", fileName, diskTime);

            // Step 2: Network I/O (OCR) - with timeout
            long t2 = Instant.now().toEpochMilli();
            String extracted = sendToOcrWithTimeout(tempPdf);
            long ocrTime = Instant.now().toEpochMilli() - t2;
            log.debug("OCR time for {}: {} ms", fileName, ocrTime);

            // Step 3: Network I/O (AI) - use timeout from Mono
            long t3 = Instant.now().toEpochMilli();
            List<ResponseModel> response = conn.process(extracted, syllabus)
                    .toFuture()
                    .get(); // Non-blocking async wait
            long aiTime = Instant.now().toEpochMilli() - t3;
            log.debug("AI processing time for {}: {} ms", fileName, aiTime);

            // Step 4: Build final result Map - ONLY send what frontend needs
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("file_name", fileName);
            fileInfo.put("content_type", file.getContentType());
            fileInfo.put("size_kb", size);
            fileInfo.put("analysis", response);  // Only the ResponseModel list

            log.info("Successfully processed {}: {} ms total", fileName, Instant.now().toEpochMilli() - fileStartTime);
            return fileInfo;

        } catch (HandleException e) {
            log.error("Processing failed for {}: {}", fileName, e.getMessage());
            return Map.of("file_name", fileName, "error", "Processing failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error for {}: {}", fileName, e.getMessage(), e);
            return Map.of("file_name", fileName, "error", "Unexpected error: " + e.getMessage());
        } finally {
            // CRITICAL: Ensure the temporary file is deleted in ALL cases.
            if (tempPdf != null && tempPdf.exists()) {
                boolean deleted = tempPdf.delete();
                log.debug("Temp file cleanup for {}: {}", fileName, deleted ? "success" : "failed");
            }
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new HandleException("PDF file missing or empty");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new HandleException(
                    "Only PDF files allowed. Received: " + file.getContentType()
            );
        }
        if (getSize(file) > 2000) {
            throw new HandleException("File " + file.getOriginalFilename() + " exceeded size limit\nRequired Limit : 2MB");
        }
    }

    // OCR

    private String sendToOcrWithTimeout(File pdfFile) {
        try {
            return sendToOcr(pdfFile);
        } catch (Exception e) {
            log.error("OCR request failed: {}", e.getMessage());
            throw new HandleException("OCR processing failed: " + e.getMessage());
        }
    }

    private String sendToOcr(File pdfFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(pdfFile));

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> resp = restTemplate.postForObject(url, entity, Map.class);

        if (resp == null || resp.get("text") == null) {
            throw new HandleException("OCR failed or returned empty text");
        }

        return resp.get("text").toString();
    }

    private File saveTempPdf(MultipartFile file) {
        try {
            File dir = new File("uploads_temp");
            if (!dir.exists()) dir.mkdirs();

            File pdf = new File(dir, System.currentTimeMillis() + "_" + file.getOriginalFilename());

            try (FileOutputStream fos = new FileOutputStream(pdf)) {
                fos.write(file.getBytes());
            }
            return pdf;
        } catch (IOException e) {
            throw new HandleException("Failed to save temp PDF " +  e );
        }
    }

    private double getSize(MultipartFile file) {
        long bytes = file.getSize();
        return Math.round((bytes / 1024.0) * 100) / 100.0;
    }
    
}
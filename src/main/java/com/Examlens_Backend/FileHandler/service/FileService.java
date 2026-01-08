package com.Examlens_Backend.FileHandler.service;

import com.Examlens_Backend.FileHandler.Models.ResponseModel;
import com.Examlens_Backend.FileHandler.Models.TopicDetails;
import com.Examlens_Backend.FileHandler.exceptions.HandleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier; // NEW
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Value("${py.url}")
    private String url;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("fileProcessingExecutor")
    private Executor executor;

    @Autowired
    private AIConnection conn ;

    private final ObjectMapper mapper = new ObjectMapper();


    public List<Map<String, Object>> handleUpload(MultipartFile[] files, List<TopicDetails>syllabus) {

        if (files == null || syllabus == null) {
            throw new HandleException("Required field is missing ! ! ");
        }
        if (syllabus.isEmpty()) {
            throw new HandleException("Syllabus is required");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            validatePdf(file);
            results.add(processSingleFile(file, syllabus));
        }
         return results;
    }

    private Map<String, Object> processSingleFile(MultipartFile file , List<TopicDetails>syllabus) {

        File tempPdf = null;
        try {
            // Step 1: Disk I/O (save)
            double size = getSize(file);
            tempPdf = saveTempPdf(file);

            // Step 2: Network I/O (OCR)
            String extracted = sendToOcr(tempPdf);

            // Step 3: Network I/O
         List<ResponseModel> response = conn.process(extracted, syllabus).block();

            // Step 4: Build final result Map
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("file_name", file.getOriginalFilename());
            fileInfo.put("content_type", file.getContentType());
            fileInfo.put("size_kb", size);
            fileInfo.put("analysis", response);

            return fileInfo;

        } catch (HandleException e) {
            return Map.of("file_name", Objects.requireNonNull(file.getOriginalFilename()), "error", "Processing failed: " + e.getMessage());
        } catch (Exception e) {
            return Map.of("file_name", Objects.requireNonNull(file.getOriginalFilename()), "error", "Unexpected error: " + e.getMessage());
        } finally {
            // CRITICAL: Ensure the temporary file is deleted in ALL cases.
            if (tempPdf != null && tempPdf.exists()) {
                tempPdf.delete();
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
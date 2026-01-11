package com.Examlens_Backend.FileHandler.controller;

import com.Examlens_Backend.FileHandler.Models.TopicDetails;
import com.Examlens_Backend.FileHandler.service.FileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;


@RestController
public class FileController {

    @Autowired
    FileService fileService ;

    @PostMapping(
            value = "/upload-pdf",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadFile(
            @RequestPart("files") MultipartFile[] files,
            @RequestPart("req") Map<String,Object> req
    ) {
        List<Map<String, Object>> topicList =
                (List<Map<String, Object>>) req.get("request");

        ObjectMapper mapper = new ObjectMapper();
        List<TopicDetails> syllabus = mapper.convertValue(
                topicList,
                new TypeReference<List<TopicDetails>>() {}
        );

        List<Map<String,Object>> details =
                fileService.handleUpload(files, syllabus);

        return ResponseEntity.status(HttpStatus.CREATED).body(details);
    }

    @GetMapping("/")
    public String HealthCheck(){
        return "Running at port 8080" ; 
    }

}

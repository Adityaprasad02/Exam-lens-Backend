package com.Examlens_Backend.FileHandler.service;

import com.Examlens_Backend.FileHandler.Models.ResponseModel;
import com.Examlens_Backend.FileHandler.Models.TopicDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
public class AIConnection {

    private static final Logger log = LoggerFactory.getLogger(AIConnection.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AIConnection(
            @Value("${spring.ai.a4j.base.url}") String aiUrl,
            @Value("${spring.ai.a4j.api.key}") String apiKey,
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${model}") String model
    ) {

        this.objectMapper = objectMapper;
        this.model = model;

        this.webClient = builder
                .baseUrl(aiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("AIConnection initialized with base URL: {}", aiUrl);
    }

    public Mono<List<ResponseModel>> process(String paperText, List<TopicDetails> syllabus) {
    String prompt = buildPrompt(paperText, syllabus);

   Map<String, Object> requestBody = Map.of(
    "model", model,
    "messages", List.of(
        Map.of("role", "system",
               "content", "You are a JSON API. Return ONLY a raw JSON object with a 'results' key containing an array. No markdown, no explanation."),
        Map.of("role", "user", "content", prompt)
    ),
    "temperature", 0,
    "max_tokens", 3000,
    "response_format", Map.of("type", "json_object") // ✅ keep this, but wrap array
);

    return webClient.post()
        .uri("/chat/completions")
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(HttpStatusCode::isError, res ->
            res.bodyToMono(String.class)
                .flatMap(err -> {
                    log.error("AI API error: {}", err);
                    return Mono.error(new RuntimeException(err));
                })
        )
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(240))
        .retryWhen(
            Retry.backoff(3, Duration.ofSeconds(2))
                .filter(e -> !(e instanceof WebClientResponseException.BadRequest))
        )
        .map(this::extractContent)
        .map(this::parseJson)
        .doOnError(e -> log.error("AI processing failed", e))
        .onErrorResume(e -> Mono.just(new ArrayList<>()));
    }

    private String buildPrompt(String paper, List<TopicDetails> topics) {

        try {

            String syllabusJson = objectMapper.writeValueAsString(topics);

            return """
You are an exam analysis AI.
Return ONLY a JSON object with a "results" key containing an array.

SYLLABUS:
%s

EXAM PAPER:
%s

Output format:
{
  "results": [
    {
      "Topic": "Main topic name",
      "Total_Marks": number,
      "Important_Subtopics": ["subtopic1", "subtopic2"]
    }
  ]
}

Rules:
- Only JSON object with "results" key
- No explanation
- No markdown
""".formatted(syllabusJson, paper);
        } catch (Exception e) {

            throw new RuntimeException("Prompt creation failed", e);
        }
    }

    private String extractContent(String rawResponse) {

        try {

            log.info("Raw AI response: {}", rawResponse);

            Map<String, Object> root =
                    objectMapper.readValue(rawResponse, new TypeReference<>() {});

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) root.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in response");
            }

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            Object content = message.get("content");

            if (content instanceof String) {
                return ((String) content).trim();
            }

            if (content instanceof List<?>) {

                List<?> arr = (List<?>) content;

                if (!arr.isEmpty()) {

                    Map<?, ?> first = (Map<?, ?>) arr.get(0);

                    Object text = first.get("text");

                    if (text != null) {
                        return text.toString().trim();
                    }
                }
            }

            throw new RuntimeException("Unsupported AI response format");

        } catch (Exception e) {

            log.error("Content extraction failed", e);

            throw new RuntimeException(e);
        }
    }

    private List<ResponseModel> parseJson(String json) {
    try {
        log.info("Parsing JSON: {}", json);

        Map<String, Object> wrapper = objectMapper.readValue(json, new TypeReference<>() {});
        Object results = wrapper.get("results");

        if (results == null) {
            log.error("No 'results' key found in AI response");
            return new ArrayList<>();
        }

        String resultsJson = objectMapper.writeValueAsString(results);
        return objectMapper.readValue(resultsJson, new TypeReference<List<ResponseModel>>() {});

    } catch (Exception e) {
        log.error("JSON parsing failed: {}", e.getMessage());
        throw new RuntimeException("Invalid JSON returned by AI");
    }
}
}
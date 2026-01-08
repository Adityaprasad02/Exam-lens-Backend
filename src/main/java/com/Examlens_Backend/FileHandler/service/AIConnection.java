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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        log.info("Initializing AI Connection with URL: {}", aiUrl);

        this.webClient = builder
                .baseUrl(aiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<List<ResponseModel>> process(String paperText, List<TopicDetails> syllabus) {
        log.debug("Processing paper with {} topics", syllabus.size());

        String prompt = buildPrompt(paperText, syllabus);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are an exam analysis AI. You MUST respond with ONLY valid JSON array. " +
                                        "No markdown, no explanations, no code blocks. Just the JSON array."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,  // Lower temperature for more consistent output
                "max_tokens", 2000
        );

        // Log the request being sent
        log.info("=== AI REQUEST ===");
        log.info("URL: {}/chat/completions", webClient.mutate().build().toString());
        log.info("Model: {}", model);
        log.info("Prompt length: {} characters", prompt.length());

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(err -> {
                                    log.error("AI API Error: {}", err);
                                    return Mono.error(new RuntimeException("AI API Error: " + err));
                                }))
                .bodyToMono(String.class)
                .doOnNext(rawResponse -> {
                    // Log the complete raw response from AI
                    log.info("=== RAW AI RESPONSE ===");
                    log.info("{}", rawResponse);
                    log.info("=== END RAW RESPONSE ===");
                })
                .timeout(Duration.ofSeconds(30))  // 30 second timeout
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2))  // Retry 2 times
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest)))
                .map(this::extractJson)
                .map(this::parseJson)
                .doOnError(e -> log.error("Failed to process AI request: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Returning empty result due to error", e);
                    return Mono.just(new ArrayList<>());  // Return empty list on error
                });
    }

    private String buildPrompt(String paper, List<TopicDetails> topics) {
        try {
            String syllabusJson = objectMapper.writeValueAsString(topics);

            return String.format("""
                Analyze this exam paper against the syllabus and identify important topics.
                
                SYLLABUS (JSON):
                %s
                
                EXAM PAPER TEXT:
                %s
                
                INSTRUCTIONS:
                1. Match questions in the paper to subtopics in the syllabus (use semantic matching)
                2. Extract the marks mentioned for each question (e.g., "2M", "5 marks", "10M")
                3. Sum up all marks for each topic to get Total_Marks
                4. List only the matched subtopic names in Important_Subtopics array
                
                OUTPUT FORMAT (respond with ONLY this JSON, nothing else):
                [
                  {
                    "Topic": "topic name from syllabus",
                    "Total_Marks": 15,
                    "Important_Subtopics": ["subtopic1", "subtopic2"]
                  }
                ]
                
                CRITICAL: Return ONLY the JSON array. No markdown, no code blocks, no explanations.
                """, syllabusJson, paper);

        } catch (Exception e) {
            log.error("Failed to build prompt", e);
            throw new RuntimeException("Prompt creation failed: " + e.getMessage());
        }
    }

    private String extractJson(String raw) {
        try {
            log.info("=== EXTRACTING JSON ===");
            log.info("Raw response length: {} characters", raw.length());

            Map<String, Object> root = objectMapper.readValue(raw, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in AI response");
            }

            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            String content = msg.get("content").toString().trim();

            log.info("=== AI GENERATED CONTENT ===");
            log.info("{}", content);
            log.info("=== END CONTENT ===");

            // Remove markdown code blocks if present
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

            int s = content.indexOf('[');
            int e = content.lastIndexOf(']');

            if (s == -1 || e == -1) {
                log.error("No JSON array found in content: {}", content);
                throw new RuntimeException("No JSON array found in AI response");
            }

            String json = content.substring(s, e + 1);
            json = autoFixJson(json);

            log.info("=== EXTRACTED & FIXED JSON ===");
            log.info("{}", json);
            log.info("=== END JSON ===");

            return json;

        } catch (Exception e) {
            log.error("JSON extraction failed", e);
            throw new RuntimeException("JSON extraction failed: " + e.getMessage());
        }
    }

    private String autoFixJson(String json) {
        // Remove trailing commas before closing brackets/braces
        json = json.replaceAll(",\\s*]", "]");
        json = json.replaceAll(",\\s*}", "}");

        // Ensure proper closing
        if (!json.trim().endsWith("]")) {
            json = json.trim() + "]";
        }

        // Balance braces
        long openBraces = json.chars().filter(ch -> ch == '{').count();
        long closeBraces = json.chars().filter(ch -> ch == '}').count();

        while (closeBraces < openBraces) {
            json = json + "}";
            closeBraces++;
        }

        return json;
    }

    private List<ResponseModel> parseJson(String json) {
        try {
            List<ResponseModel> result = objectMapper.readValue(json, new TypeReference<>() {});
            log.info("Successfully parsed {} response items", result.size());
            return result;
        } catch (Exception e) {
            log.error("JSON parse error. JSON was: {}", json, e);
            throw new RuntimeException("JSON parse error: " + e.getMessage());
        }
    }
}
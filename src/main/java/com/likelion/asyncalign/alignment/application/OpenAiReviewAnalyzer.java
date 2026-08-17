package com.likelion.asyncalign.alignment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.likelion.asyncalign.alignment.domain.ConfidenceLevel;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiReviewAnalyzer implements AiReviewAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(OpenAiReviewAnalyzer.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiReviewAnalyzer(
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Analysis analyze(AnalysisInput input) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(input, "LOCAL_FALLBACK");
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody(input))
                    .retrieve()
                    .body(JsonNode.class);
            String outputText = extractOutputText(response);
            JsonNode result = objectMapper.readTree(outputText);
            return new Analysis(
                    textOrDefault(result, "sourceLanguage", detectLanguage(input.content())),
                    nullableText(result, "translatedContent"),
                    nullableText(result, "task"),
                    confidence(result, "taskConfidence"),
                    instant(result, "deadline"),
                    confidence(result, "deadlineConfidence"),
                    nullableText(result, "expectedOutcome"),
                    confidence(result, "expectedOutcomeConfidence"),
                    "OPENAI");
        } catch (Exception exception) {
            log.warn("OpenAI review analysis failed; using safe fallback: {}", exception.getMessage());
            return fallback(input, "LOCAL_FALLBACK_AFTER_AI_FAILURE");
        }
    }

    private ObjectNode requestBody(AnalysisInput input) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("instructions", """
                You extract explicit work-request conditions for a human confirmation workflow.
                Never infer nationality, culture, personality, or unstated intent.
                If a deadline is relative or lacks an exact date/time/timezone, return deadline=null.
                If a value is uncertain or absent, return null and LOW or UNKNOWN confidence.
                Translate only when sender and recipient languages differ.
                Return only the requested structured output.
                """);
        body.put("input", """
                Sender language: %s
                Recipient language: %s
                Recipient user ID: %s
                Draft: %s
                Recent messages: %s
                Attachment context: %s
                """.formatted(
                input.senderLanguage(),
                input.recipientLanguage(),
                input.recipientId(),
                input.content(),
                input.recentMessages(),
                input.attachmentContexts()));

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putArray("required").addAll(List.of(
                objectMapper.getNodeFactory().textNode("sourceLanguage"),
                objectMapper.getNodeFactory().textNode("translatedContent"),
                objectMapper.getNodeFactory().textNode("task"),
                objectMapper.getNodeFactory().textNode("taskConfidence"),
                objectMapper.getNodeFactory().textNode("deadline"),
                objectMapper.getNodeFactory().textNode("deadlineConfidence"),
                objectMapper.getNodeFactory().textNode("expectedOutcome"),
                objectMapper.getNodeFactory().textNode("expectedOutcomeConfidence")));
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.set("sourceLanguage", stringSchema(false));
        properties.set("translatedContent", stringSchema(true));
        properties.set("task", stringSchema(true));
        properties.set("taskConfidence", confidenceSchema());
        ObjectNode deadline = stringSchema(true);
        deadline.put("description", "Exact ISO-8601 UTC instant, or null when ambiguous");
        properties.set("deadline", deadline);
        properties.set("deadlineConfidence", confidenceSchema());
        properties.set("expectedOutcome", stringSchema(true));
        properties.set("expectedOutcomeConfidence", confidenceSchema());

        ObjectNode format = body.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "work_request_review");
        format.put("strict", true);
        format.set("schema", schema);
        return body;
    }

    private ObjectNode stringSchema(boolean nullable) {
        ObjectNode node = objectMapper.createObjectNode();
        if (nullable) {
            node.putArray("type").add("string").add("null");
        } else {
            node.put("type", "string");
        }
        return node;
    }

    private ObjectNode confidenceSchema() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "string");
        node.putArray("enum").add("HIGH").add("MEDIUM").add("LOW").add("UNKNOWN");
        return node;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null || !response.path("status").asText().equals("completed")) {
            throw new IllegalStateException("OpenAI response did not complete");
        }
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("OpenAI response has no output_text");
    }

    private Analysis fallback(AnalysisInput input, String provider) {
        String content = input.content().trim();
        String sourceLanguage = detectLanguage(content);
        String task = content.length() > 500 ? content.substring(0, 500) : content;
        boolean ambiguousDeadline = content.matches("(?s).*?(내일|이번 주|오늘 중|ASAP|tomorrow|this week).*?");
        return new Analysis(
                sourceLanguage,
                null,
                task,
                ConfidenceLevel.MEDIUM,
                null,
                ambiguousDeadline ? ConfidenceLevel.LOW : ConfidenceLevel.UNKNOWN,
                null,
                ConfidenceLevel.UNKNOWN,
                provider);
    }

    private String detectLanguage(String content) {
        return content.codePoints().anyMatch(codePoint -> codePoint >= 0xAC00 && codePoint <= 0xD7A3)
                ? "ko"
                : "en";
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        String value = nullableText(node, field);
        return value == null ? defaultValue : value;
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private ConfidenceLevel confidence(JsonNode node, String field) {
        try {
            return ConfidenceLevel.valueOf(node.path(field).asText("UNKNOWN"));
        } catch (IllegalArgumentException exception) {
            return ConfidenceLevel.UNKNOWN;
        }
    }

    private Instant instant(JsonNode node, String field) {
        String value = nullableText(node, field);
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }
}

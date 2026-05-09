package com.nidextractor.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * OCR engine using Anthropic Claude's Vision API.
 * Highly accurate for mixed Bengali + English documents.
 */
public class ClaudeVisionEngine implements OCREngine {

    private static final Logger log = LoggerFactory.getLogger(ClaudeVisionEngine.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-opus-4-5";

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public ClaudeVisionEngine(String apiKey) {
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String extractText(File imageFile) throws IOException {
        log.info("Sending image to Claude Vision API: {}", imageFile.getName());

        // Convert image to base64
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mediaType = detectMediaType(imageFile.getName());

        // Build request body
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", 1024);

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");

        ArrayNode content = userMessage.putArray("content");

        // Image block
        ObjectNode imageBlock = content.addObject();
        imageBlock.put("type", "image");
        ObjectNode source = imageBlock.putObject("source");
        source.put("type", "base64");
        source.put("media_type", mediaType);
        source.put("data", base64Image);

        // Prompt block
        ObjectNode textBlock = content.addObject();
        textBlock.put("type", "text");
        textBlock.put("text",
                "This is a Bangladesh National ID Card. " +
                        "Extract ALL text exactly as it appears, preserving Bengali Unicode characters. " +
                        "Return only the raw extracted text, nothing else."
        );

        // HTTP call
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(
                        mapper.writeValueAsString(requestBody),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Claude API error: " + response.code() + " " + response.body().string());
            }
            JsonNode json = mapper.readTree(response.body().string());
            String text = json.path("content").get(0).path("text").asText();
            log.debug("Claude raw response:\n{}", text);
            return text;
        }
    }

    private String detectMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    @Override
    public String getEngineName() {
        return "Claude Vision (Anthropic API)";
    }
}
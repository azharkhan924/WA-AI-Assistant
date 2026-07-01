package com.whatsappai.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrapper around Google Gemini and Groq API with multi-key rotation and fallbacks.
 * The provider used is "gemini" or "groq", chosen globally via app.ai.provider
 * or per-business via BusinessProfile.aiProvider.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicInteger currentGeminiKeyIndex = new AtomicInteger(0);
    private final AtomicInteger currentGroqKeyIndex = new AtomicInteger(0);

    @Value("${app.ai.provider}")
    private String defaultProvider;

    @Value("${app.ai.max-tokens}")
    private int maxTokens;

    @Value("${app.ai.gemini.api-keys}")
    private String geminiApiKeysStr;

    @Value("${app.ai.gemini.model}")
    private String geminiModel;

    @Value("${app.ai.gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${app.ai.groq.api-keys}")
    private String groqApiKeysStr;

    @Value("${app.ai.groq.model}")
    private String groqModel;

    @Value("${app.ai.groq.base-url}")
    private String groqBaseUrl;

    /**
     * @param systemPrompt    the fully assembled business system prompt
     * @param conversationLog prior turns, oldest first, alternating user/assistant text
     * @param userMessage     the newest customer message
     * @param providerOverride per-business override of "gemini"/"groq"; nullable
     */
    public String generateReply(String systemPrompt, List<String[]> conversationLog, String userMessage, String providerOverride) {
        String provider = (providerOverride != null && !providerOverride.isBlank()) ? providerOverride : defaultProvider;

        try {
            if ("groq".equalsIgnoreCase(provider)) {
                return callGroq(systemPrompt, conversationLog, userMessage);
            }
            return callGemini(systemPrompt, conversationLog, userMessage);
        } catch (Exception e) {
            log.error("AI provider call failed", e);
            return "Sorry, I'm having trouble responding right now. Please try again shortly, or contact us directly for help.";
        }
    }

    private String callGemini(String systemPrompt, List<String[]> log, String userMessage) {
        if (geminiApiKeysStr == null || geminiApiKeysStr.isBlank()) {
            return "AI is not configured yet. Please set GEMINI_API_KEYS on the server.";
        }

        String[] keys = geminiApiKeysStr.split(",");
        if (keys.length == 0 || keys[0].isBlank()) {
            return "AI is not configured yet. Please set GEMINI_API_KEYS on the server.";
        }

        ObjectNode body = mapper.createObjectNode();

        // systemInstruction
        ObjectNode systemInstruction = mapper.createObjectNode();
        ArrayNode sysParts = mapper.createArrayNode();
        ObjectNode sysPart = mapper.createObjectNode();
        sysPart.put("text", systemPrompt);
        sysParts.add(sysPart);
        systemInstruction.set("parts", sysParts);
        body.set("systemInstruction", systemInstruction);

        // contents
        ArrayNode contents = mapper.createArrayNode();
        for (String[] turn : log) {
            ObjectNode c = mapper.createObjectNode();
            c.put("role", "assistant".equalsIgnoreCase(turn[0]) ? "model" : "user");
            ArrayNode parts = mapper.createArrayNode();
            ObjectNode part = mapper.createObjectNode();
            part.put("text", turn[1]);
            parts.add(part);
            c.set("parts", parts);
            contents.add(c);
        }
        ObjectNode current = mapper.createObjectNode();
        current.put("role", "user");
        ArrayNode currentParts = mapper.createArrayNode();
        ObjectNode currentPart = mapper.createObjectNode();
        currentPart.put("text", userMessage);
        currentParts.add(currentPart);
        current.set("parts", currentParts);
        contents.add(current);
        body.set("contents", contents);

        // generationConfig
        ObjectNode genConfig = mapper.createObjectNode();
        genConfig.put("maxOutputTokens", maxTokens);
        body.set("generationConfig", genConfig);

        int startIdx = currentGeminiKeyIndex.get();
        Exception lastError = null;

        for (int i = 0; i < keys.length; i++) {
            int idx = (startIdx + i) % keys.length;
            String apiKey = keys[idx].trim();
            String url = geminiBaseUrl + "/" + geminiModel + ":generateContent?key=" + apiKey;

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                // If successful, save the index
                currentGeminiKeyIndex.set(idx);

                JsonNode root = parse(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText("").trim();
                    }
                }
                throw new RuntimeException("Gemini API returned empty/invalid response");
            } catch (Exception e) {
                lastError = e;
                AiService.log.warn("Gemini API key index {} failed: {}", idx, e.getMessage());
                // Rotate to next key on next call
                currentGeminiKeyIndex.set((idx + 1) % keys.length);
            }
        }
        throw new RuntimeException("All Gemini API keys failed", lastError);
    }

    private String callGroq(String systemPrompt, List<String[]> log, String userMessage) {
        if (groqApiKeysStr == null || groqApiKeysStr.isBlank()) {
            return "AI is not configured yet. Please set GROQ_API_KEYS on the server.";
        }

        String[] keys = groqApiKeysStr.split(",");
        if (keys.length == 0 || keys[0].isBlank()) {
            return "AI is not configured yet. Please set GROQ_API_KEYS on the server.";
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("model", groqModel);
        body.put("max_tokens", maxTokens);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        messages.add(sys);

        for (String[] turn : log) {
            ObjectNode m = mapper.createObjectNode();
            m.put("role", turn[0]);
            m.put("content", turn[1]);
            messages.add(m);
        }
        ObjectNode current = mapper.createObjectNode();
        current.put("role", "user");
        current.put("content", userMessage);
        messages.add(current);

        body.set("messages", messages);

        int startIdx = currentGroqKeyIndex.get();
        Exception lastError = null;

        for (int i = 0; i < keys.length; i++) {
            int idx = (startIdx + i) % keys.length;
            String apiKey = keys[idx].trim();

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(groqBaseUrl, entity, String.class);

                // If successful, save the index
                currentGroqKeyIndex.set(idx);

                JsonNode root = parse(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    return choices.get(0).path("message").path("content").asText("").trim();
                }
                throw new RuntimeException("Groq API returned empty/invalid response");
            } catch (Exception e) {
                lastError = e;
                AiService.log.warn("Groq API key index {} failed: {}", idx, e.getMessage());
                // Rotate to next key on next call
                currentGroqKeyIndex.set((idx + 1) % keys.length);
            }
        }
        throw new RuntimeException("All Groq API keys failed", lastError);
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            log.error("Failed to parse AI response JSON", e);
            return mapper.createObjectNode();
        }
    }
}

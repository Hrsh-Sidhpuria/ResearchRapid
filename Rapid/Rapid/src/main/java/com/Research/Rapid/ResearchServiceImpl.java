package com.Research.Rapid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ResearchServiceImpl implements ResearchService {

    private final String geminiApiUrl;
    private final String geminiApiKey;
    private final WebClient webClient;

    public ResearchServiceImpl(
            WebClient.Builder webClientBuilder,
            @Value("${gemini.api.url}") String geminiApiUrl,
            @Value("${gemini.api.key}") String geminiApiKey) {

        this.geminiApiUrl = geminiApiUrl != null ? geminiApiUrl.trim() : null;
        this.geminiApiKey = geminiApiKey != null ? geminiApiKey.trim() : null;

        this.webClient = webClientBuilder
                .baseUrl(this.geminiApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String ProcessContent(ResearchRequest request) {
        String prompt = buildPrompt(request);

        // Map directly to your cURL -d payload body structure
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> partsContainer = new HashMap<>();
        partsContainer.put("parts", Collections.singletonList(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(partsContainer));

        // Using your exact curl target model ID
        String modelName = "gemini-flash-latest";

        // Query parameter strategy ensures Netty never triggers 0x20 header exceptions
        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/" + modelName + ":generateContent")
                        .queryParam("key", this.geminiApiKey)
                        .build())
                .bodyValue(requestBody)
                .retrieve() 
                .bodyToMono(String.class)
                .block();

        return extractTextFromResponse(response);
    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();
        String operation = request.getOperation();

        switch (operation) {
            case "summarize":
            case "summerize":
                prompt.append("Act as an expert summarizer. Summarize the following text in 3 concise bullet points. Keep the tone professional and simple:\n\n");
                break;
            case "suggest":
                prompt.append("Based on the content below, recommend 3 relevant articles or books. For each, provide the title and a brief explanation of why it connects to this content.\n\n");
                break;
            default:
                throw new IllegalStateException("Unexpected operation: " + operation);
        }
        prompt.append(request.getContent());
        return prompt.toString();
    }

    private String extractTextFromResponse(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            // Parse out content directly from the standard "candidates" JSON tree array
            if (rootNode.has("candidates")) {
                JsonNode candidates = rootNode.get("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode content = candidates.get(0).path("content");
                    if (content.has("parts")) {
                        JsonNode parts = content.get("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            return parts.get(0).path("text").asText();
                        }
                    }
                }
            }

            System.err.println("Unexpected Response Structure: " + rootNode.toPrettyString());
            return "Error: Unexpected response format. Check console logs for raw JSON.";

        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }
}

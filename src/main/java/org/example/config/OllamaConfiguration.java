package org.example.config;


import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "spring.ai.ollama", ignoreUnknownFields = true)
public class OllamaConfiguration {


    private String baseUrl;

    private Map<String, String> chat;
    private Map<String, String> embedding;

    public String getBaseUrl() {
        return baseUrl;
    }

    public Map<String, String> getChat() {
        return chat;
    }

    public Map<String, String> getEmbedding() {
        return embedding;
    }
}

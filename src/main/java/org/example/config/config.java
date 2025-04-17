package org.example.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class config {
    Map<String, String> chat;
    Map<String, String> embedding;
    String baseUrl;

    public config(OllamaConfiguration config) {
        chat = config.getChat();
        embedding = config.getEmbedding();
        baseUrl = config.getBaseUrl();

    }

    @Bean
    public OllamaChatModel.Builder chatModel(OllamaConfiguration config) {
        return OllamaChatModel.builder().defaultOptions(OllamaOptions.builder().model(chat.get("model")).build());
    }



}

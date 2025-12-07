package com.seregamazur.pulse.enrichment;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jetbrains.annotations.NotNull;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.seregamazur.pulse.reading.model.RawNews;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatGPTClient {

    @Inject
    private OpenAIClient aiClient;

    @Inject
    @ConfigProperty(name = "gpt.prompt")
    private String prompt;

    @Inject
    @ConfigProperty(name = "gpt.key")
    private String key;

    @Inject
    @ConfigProperty(name = "gpt.model")
    private String model;

    @Inject
    @ConfigProperty(name = "gpt.max-tokens")
    private int maxOutputTokens;

    public @NotNull ChatCompletion getChatAnalysisResponse(RawNews rawNews) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(model)
            .addUserMessage(prompt.formatted(rawNews.title(), rawNews.text()))
            .maxTokens(maxOutputTokens)
            .temperature(0.0)
            .build();

        return aiClient.chat().completions().create(params);
    }

    @ApplicationScoped
    @Produces
    OpenAIClient client() {
        return OpenAIOkHttpClient.builder()
            .apiKey(key)
            .build();
    }
}

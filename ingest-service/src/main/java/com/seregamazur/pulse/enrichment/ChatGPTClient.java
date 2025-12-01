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
    OpenAIClient aiClient;

    @Inject
    @ConfigProperty(name = "gpt.prompt")
    String prompt;

    @Inject
    @ConfigProperty(name = "gpt.key")
    String key;

    @Inject
    @ConfigProperty(name = "gpt.model")
    String model;

    @Inject
    @ConfigProperty(name = "gpt.max-tokens")
    int maxTokens;

    public @NotNull ChatCompletion getChatAnalysisResponse(RawNews rawNews) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(model)
            .addUserMessage(prompt.formatted(rawNews.title(), rawNews.text()))
            .maxTokens(maxTokens)
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

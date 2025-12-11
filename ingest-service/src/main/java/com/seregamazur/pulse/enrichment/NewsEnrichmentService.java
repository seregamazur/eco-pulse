package com.seregamazur.pulse.enrichment;

import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.errors.BadRequestException;
import com.openai.models.chat.completions.ChatCompletion;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.reading.model.RawNews;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class NewsEnrichmentService {

    @Inject
    private ChatGPTClient chatGPTClient;
    @Inject
    private ObjectMapper mapper;

    public Optional<EnrichedNewsDocument> enrich(RawNews raw) {
        try {
            ChatCompletion result = chatGPTClient.getChatAnalysisResponse(raw);
            EnrichedNewsDocument enrichedNewsDocument = null;
            enrichedNewsDocument = mapper.readValue(
                result.choices().getFirst().message().content().orElse("{}"),
                EnrichedNewsDocument.class);
            enrichedNewsDocument.setDate(raw.date());
            enrichedNewsDocument.setIngestDate(LocalDate.now());
            return Optional.of(enrichedNewsDocument);
        } catch (JsonProcessingException | BadRequestException e) {
            log.error("Failed to enrich news: {}", raw.title(), e);
            return Optional.empty();
        }
    }
}
package com.seregamazur.pulse.indexing;

import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openai.models.chat.completions.ChatCompletion;
import com.seregamazur.pulse.enrichment.ChatGPTClient;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.indexing.model.IndexResult;
import com.seregamazur.pulse.reading.model.RawNews;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Processor {

    @Inject
    ChatGPTClient chatGPTClient;

    @Inject
    Indexer indexService;

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public IndexResult enrichAndIndex(RawNews raw) {
        try {
            EnrichedNewsDocument doc = enrich(raw);
            indexService.index(doc);
            return IndexResult.ok(raw.title());
        } catch (Exception e) {
            System.err.println("Error processing: " + raw.title());
            return IndexResult.fail(raw.title(), e);
        }
    }

    private EnrichedNewsDocument enrich(RawNews raw) {
        ChatCompletion result = chatGPTClient.getChatAnalysisResponse(raw);
        EnrichedNewsDocument enrichedNewsDocument = null;
        try {
            enrichedNewsDocument = mapper.readValue(
                result.choices().get(0).message().content().orElse("{}"),
                EnrichedNewsDocument.class);
            enrichedNewsDocument.setDate(raw.date());
            enrichedNewsDocument.setIngestDate(LocalDate.now());
            return enrichedNewsDocument;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

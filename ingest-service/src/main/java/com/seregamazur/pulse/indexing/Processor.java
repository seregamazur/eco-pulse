package com.seregamazur.pulse.indexing;

import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletion;
import com.seregamazur.pulse.enrichment.ChatGPTClient;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.indexing.model.IndexResult;
import com.seregamazur.pulse.reading.model.RawNews;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class Processor {

    @Inject
    private ChatGPTClient chatGPTClient;

    @Inject
    private Indexer indexService;

    @Inject
    private ObjectMapper mapper;

    public IndexResult enrichAndIndex(RawNews raw) {
        try {
            EnrichedNewsDocument doc = enrich(raw);
            indexService.index(doc);
            return IndexResult.ok(raw.title());
        } catch (Exception e) {
            log.error("Error processing: {}", raw.title());
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
            throw new RuntimeException("An exception occurred when trying to parse ChatGPT response!", e);
        }
    }
}

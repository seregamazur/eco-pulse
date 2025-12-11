package com.seregamazur.pulse.pipeline;

import java.util.Optional;

import com.seregamazur.pulse.enrichment.NewsEnrichmentService;
import com.seregamazur.pulse.indexing.NewsIndexingService;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.indexing.model.IndexResult;
import com.seregamazur.pulse.reading.model.RawNews;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class NewsProcessingOrchestrator {

    @Inject
    private NewsEnrichmentService enrichmentService;
    @Inject
    private NewsIndexingService indexingService;

    public IndexResult enrichAndIndex(RawNews raw) {
        try {
            Optional<EnrichedNewsDocument> doc = enrichmentService.enrich(raw);

            if (doc.isEmpty()) {
                log.warn("Skipping processing due to empty enrichment for: {}", raw.title());
                return IndexResult.skip(raw.title());
            }

            return indexingService.indexDocument(doc.get());

        } catch (Exception e) {
            log.error("Error processing: {}", raw.title());
            return IndexResult.fail(raw.title(), e);
        }
    }
}
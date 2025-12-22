package com.seregamazur.pulse.indexing;

import java.util.List;

import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.indexing.model.IndexResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class NewsIndexingService {

    @Inject
    private Indexer indexer;

    public IndexResult indexDocument(EnrichedNewsDocument doc) {
        try {
            indexer.index(doc);
            return IndexResult.ok(doc.getTitle());
        } catch (Exception e) {
            log.error("Error indexing: {}", doc.getTitle());
            return IndexResult.fail(doc.getTitle(), e);
        }
    }

    public List<IndexResult> bulkIndexDocuments(List<EnrichedNewsDocument> doc) {
        try {
            return indexer.bulkIndex(doc);
        } catch (Exception e) {
            log.error("Unexpected exception while indexing batch");
            throw new RuntimeException(e);
        }
    }
}
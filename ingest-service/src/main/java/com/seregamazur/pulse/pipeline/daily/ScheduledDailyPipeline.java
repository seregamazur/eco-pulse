package com.seregamazur.pulse.pipeline.daily;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.seregamazur.pulse.enrichment.NewsEnrichmentService;
import com.seregamazur.pulse.indexing.Indexer;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.indexing.model.IndexResult;
import com.seregamazur.pulse.reading.EnrichedNewsJsonMapper;
import com.seregamazur.pulse.reading.model.RawNews;
import com.seregamazur.pulse.reading.s3.S3FileService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ScheduledDailyPipeline {

    @Inject
    private GuardianService service;

    @Inject
    private NewsEnrichmentService enrichmentService;

    @Inject
    private EnrichedNewsJsonMapper jsonMapper;

    @Inject
    private S3FileService s3FileService;

    @Inject
    private Indexer indexer;

    public CompletableFuture<List<IndexResult>> run(ExecutorService executor) {
        return service.getDailyNews().thenCompose(news -> processTodaysNews(news, executor));
    }

    /**
     * Enrich today news, and write that as json to S3
     */
    private CompletableFuture<List<IndexResult>> processTodaysNews(List<RawNews> news, ExecutorService executor) {
        List<CompletableFuture<Optional<EnrichedNewsDocument>>> tasks = news.stream()
            .map(n -> processSingleRawNews(n, executor))
            .toList();

        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> tasks.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList()
            )
            .thenCompose(enrichedList -> {
                if (enrichedList.isEmpty()) {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                byte[] allNewsBytes = jsonMapper.toBytes(enrichedList);

                return s3FileService.uploadDailyNews(allNewsBytes)
                    .thenApply(putResponse -> indexer.bulkIndex(enrichedList))
                    .handle((result, ex) -> {
                        if (ex != null) {
                            log.warn("OpenSearch indexing skipped or failed: {}. Data is safe in S3.", ex.getMessage());
                            return Collections.emptyList();
                        }
                        return result;
                    });
            });
    }

    private CompletableFuture<Optional<EnrichedNewsDocument>> processSingleRawNews(RawNews news, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> enrichmentService.enrich(news), executor)
            .toCompletableFuture();
    }
}

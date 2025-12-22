package com.seregamazur.pulse.pipeline.backfill;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.seregamazur.pulse.indexing.NewsIndexingService;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.indexing.model.IndexResult;
import com.seregamazur.pulse.reading.EnrichedNewsJsonMapper;
import com.seregamazur.pulse.reading.s3.S3FileService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class BackfillPipeline {

    @Inject
    private S3FileService s3;

    @Inject
    private NewsIndexingService indexer;

    @Inject
    private EnrichedNewsJsonMapper jsonMapper;

    public CompletableFuture<List<IndexResult>> run(LocalDate after, ExecutorService executor) {
        return s3.listEnrichedNewsFiles(after)
            .thenCompose(files -> processFiles(files, executor));
    }

    private CompletableFuture<List<IndexResult>> processFiles(List<String> files, ExecutorService executor) {
        List<CompletableFuture<List<IndexResult>>> tasks = files.stream()
            .map(key -> readAndIndexSingleNewsFile(key, executor))
            .toList();

        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> tasks.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList()
            );
    }

    private CompletableFuture<List<IndexResult>> readAndIndexSingleNewsFile(String key, ExecutorService executor) {
        return s3.downloadFile(key)
            //parse csv in virtual threads
            .thenApplyAsync(bytes -> jsonMapper.readEnrichedNews(bytes.asByteArray()), executor)
            //enrich and index in virtual threads
            .thenCompose(news -> batchIndex(news, executor));
    }

    /**
     * Splits a list of enriched documents into smaller batches and indexes them asynchronously.
     * <p>
     * This method implements a "Scatter-Gather" pattern:
     * 1. <b>Scatter:</b> The input list is partitioned into chunks (default size: 100).
     * 2. <b>Process:</b> Each chunk is submitted to the {@link ExecutorService} (Virtual Threads)
     * to be indexed via a single Bulk API request.
     * 3. <b>Gather:</b> It waits for all parallel operations to complete and aggregates
     * the individual {@link IndexResult} objects into a unified list.
     * </p>
     *
     * @param news     The list of documents to be indexed.
     * @param executor The {@link ExecutorService} providing virtual threads for parallel execution.
     * @return A {@link CompletableFuture} containing the aggregated list of results
     * from all bulk operations.
     */
    private CompletableFuture<List<IndexResult>> batchIndex(List<EnrichedNewsDocument> news, ExecutorService executor) {
        int batchSize = 100;

        List<List<EnrichedNewsDocument>> batches = new ArrayList<>();
        for (int i = 0; i < news.size(); i += batchSize) {
            batches.add(news.subList(i, Math.min(i + batchSize, news.size())));
        }

        List<CompletableFuture<List<IndexResult>>> futureResults = batches.stream()
            .map(batch -> CompletableFuture.supplyAsync(() -> indexer.bulkIndexDocuments(batch), executor))
            .toList();

        return CompletableFuture.allOf(futureResults.toArray(new CompletableFuture[0]))
            .thenApply(v -> futureResults.stream()
                .flatMap(f -> f.join().stream())
                .toList()
            )
            .exceptionally(e -> {
                log.error("Failed to index batch news!", e);
                return List.of();
            });
    }
}

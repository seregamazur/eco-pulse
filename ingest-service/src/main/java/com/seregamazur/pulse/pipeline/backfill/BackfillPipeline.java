package com.seregamazur.pulse.pipeline.backfill;

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

    public CompletableFuture<List<IndexResult>> run(ExecutorService executor) {
        return s3.listEnrichedNewsFiles().thenCompose(files -> processFiles(files, executor));
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
            .thenApplyAsync(bytes -> EnrichedNewsJsonMapper.readEnrichedNews(bytes.asByteArray()), executor)
            //enrich and index in virtual threads
            .thenCompose(news -> indexDailyNews(news, key, executor));
    }

    private CompletableFuture<List<IndexResult>> indexDailyNews(List<EnrichedNewsDocument> news, String day, ExecutorService executor) {
        List<CompletableFuture<IndexResult>> futureResult = news.stream()
            .map(doc -> CompletableFuture.supplyAsync(() -> indexer.indexDocument(doc), executor))
            .toList();

        return CompletableFuture.allOf(futureResult.toArray(new CompletableFuture[0]))
            .thenApply(v -> futureResult.stream()
                .map(CompletableFuture::join)
                .toList()
            )
            .exceptionally(e -> {
                log.error("Failed to index news for day={}", day, e);
                return List.of();
            });
    }
}

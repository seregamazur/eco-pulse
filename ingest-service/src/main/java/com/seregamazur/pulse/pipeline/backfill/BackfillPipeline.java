package com.seregamazur.pulse.pipeline.backfill;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.seregamazur.pulse.indexing.Processor;
import com.seregamazur.pulse.indexing.model.IndexResult;
import com.seregamazur.pulse.reading.RawNewsCsvParser;
import com.seregamazur.pulse.reading.model.RawNews;
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
    private Processor processor;

    public CompletableFuture<List<IndexResult>> run(ExecutorService executor) {
        return s3.listCsvFiles()
            .thenCompose(files -> processFiles(files, executor));
    }

    private CompletableFuture<List<IndexResult>> processFiles(List<String> files, ExecutorService executor) {
        List<CompletableFuture<List<IndexResult>>> tasks = files.stream()
            .map(key -> processSingleFile(key, executor))
            .toList();

        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> tasks.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList()
            );
    }

    private CompletableFuture<List<IndexResult>> processSingleFile(String key, ExecutorService executor) {
        return s3.downloadFile(key)
            //parse csv in virtual threads
            .thenApplyAsync(bytes -> RawNewsCsvParser.parseFromS3Object(key, bytes), executor)
            //enrich and index in virtual threads
            .thenCompose(news -> processRawNews(news, executor));
    }

    private CompletableFuture<List<IndexResult>> processRawNews(List<RawNews> news, ExecutorService executor) {
        List<CompletableFuture<IndexResult>> futureResult = news.stream()
            .map(row -> CompletableFuture.supplyAsync(() -> processor.enrichAndIndex(row), executor))
            .toList();

        return CompletableFuture.allOf(futureResult.toArray(new CompletableFuture[0]))
            .thenApply(v -> futureResult.stream()
                .map(CompletableFuture::join)
                .toList()
            )
            .exceptionally(e -> {
                log.error("Failed to process news", e);
                return null;
            });
    }
}

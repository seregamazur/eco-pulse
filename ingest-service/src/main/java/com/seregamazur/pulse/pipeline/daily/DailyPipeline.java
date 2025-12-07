package com.seregamazur.pulse.pipeline.daily;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.seregamazur.pulse.indexing.Processor;
import com.seregamazur.pulse.indexing.model.IndexResult;
import com.seregamazur.pulse.reading.model.RawNews;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class DailyPipeline {

    @Inject
    private GuardianService service;

    @Inject
    private Processor processor;

    public CompletableFuture<List<IndexResult>> run(ExecutorService executor) {
        return service.getDailyNews()
            .thenCompose(news -> processTodaysNews(news, executor));
    }

    private CompletableFuture<List<IndexResult>> processTodaysNews(List<RawNews> news, ExecutorService executor) {
        List<CompletableFuture<IndexResult>> tasks = news.stream()
            .map(key -> processRawNews(key, executor))
            .toList();

        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> tasks.stream()
                .map(CompletableFuture::join)
                .toList()
            );
    }

    private CompletableFuture<IndexResult> processRawNews(RawNews news, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> processor.enrichAndIndex(news), executor)
            .exceptionally(e -> {
                log.error("Failed to process news: {} - {}", news.title(), e.getMessage());
                return null;
            })
            .toCompletableFuture();
    }
}

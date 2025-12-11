package com.seregamazur.pulse.pipeline.historical;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seregamazur.pulse.enrichment.NewsEnrichmentService;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.reading.EnrichedNewsJsonMapper;
import com.seregamazur.pulse.reading.RawNewsCsvParser;
import com.seregamazur.pulse.reading.model.RawNews;
import com.seregamazur.pulse.reading.s3.S3FileService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class HistoricalDataEnrichmentPipeline {

    @Inject
    private S3FileService s3;

    @Inject
    private NewsEnrichmentService enrichmentService;

    private final ExecutorService PARSE_CSV_EXECUTOR = Executors.newFixedThreadPool(100, Thread.ofVirtual().factory());

    //due to RPM/TPM of GPT limits
    private final ExecutorService ENRICH_AND_WRITE_EXECUTOR = Executors.newFixedThreadPool(2, Thread.ofVirtual().factory());

    public CompletableFuture<List<Void>> run() {
        return s3.listRawNewsFiles().thenCompose(this::processFiles);
    }

    private CompletableFuture<List<Void>> processFiles(List<String> files) {
        List<CompletableFuture<Void>> tasks = files.stream()
            .map(key -> processSingleFile(key, PARSE_CSV_EXECUTOR))
            .toList();

        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> tasks.stream()
                .map(CompletableFuture::join)
                .toList()
            );
    }

    private CompletableFuture<Void> processSingleFile(String key, ExecutorService executor) {
        return s3.downloadFile(key)
            //parse csv in virtual threads
            .thenApplyAsync(bytes -> RawNewsCsvParser.parseFromS3Object(key, bytes), executor)
            //enrich and index in virtual threads
            .thenCompose(this::enrichAndSaveSingleFile);
    }

    private CompletableFuture<Void> enrichAndSaveSingleFile(List<RawNews> news) {
        List<CompletableFuture<Optional<EnrichedNewsDocument>>> futureResult = news.stream()
            .map(n -> CompletableFuture.supplyAsync(() -> enrichmentService.enrich(n), ENRICH_AND_WRITE_EXECUTOR))
            .toList();

        return CompletableFuture.allOf(futureResult.toArray(new CompletableFuture[0]))
            .thenApply(v -> futureResult.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList()
            )
            .thenAcceptAsync(EnrichedNewsJsonMapper::writeEnrichedToFile);
    }

}

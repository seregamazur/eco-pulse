package com.seregamazur.pulse.pipeline.daily;

import java.util.List;
import java.util.concurrent.Executors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.seregamazur.pulse.indexing.model.IndexResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

/**
 * AWS Lambda handler for the automated daily news processing task.
 * <p>
 * Triggered every morning via EventBridge
 * this handler manages the complete workflow for the previous day's news:
 * 1. <b>Ingestion:</b> Fetches raw news content from The Guardian API.
 * 2. <b>Sanitization:</b> Cleans HTML tags and normalizes text.
 * 3. <b>Enrichment:</b> Processes text via LLM for advanced metadata.
 * 4. <b>Indexing:</b> Batches and stores the final documents in OpenSearch.
 * </p>
 *
 * <b>Implementation Details:</b>
 * <ul>
 * <li>Uses {@code newThreadPerTaskExecutor} for unbounded virtual thread creation,
 * leveraging the lower volume of daily news compared to {@link com.seregamazur.pulse.pipeline.backfill.BackfillIndexingLambdaHandler}</li>
 * <li>The process is fully asynchronous, utilizing {@link java.util.concurrent.CompletableFuture}
 * aggregation for result tracking.</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
@Named("daily-lambda")
public class ScheduledDailyLambdaHandler implements RequestHandler<Object, Integer> {
    @Inject
    private ScheduledDailyPipeline pipeline;

    @Override
    public Integer handleRequest(Object input, Context context) {
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            long start = System.currentTimeMillis();
            List<IndexResult> results = pipeline.run(executor).join();
            long end = System.currentTimeMillis();

            long successCount = results.stream().filter(IndexResult::success).count();
            long failCount = results.size() - successCount;

            log.info("Daily job finished. Total: {}, Success: {}, Failed: {}, Took: {}", results.size(), successCount, failCount, end - start);

            if (failCount > 0) {
                log.warn("Failures:");
                results.stream()
                    .filter(r -> !r.success())
                    .forEach(r -> log.warn("- {}: {}", r.title(), r.errorMessage()));
            }
        }
        return 0;
    }
}
package com.seregamazur.pulse.pipeline.backfill;

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
 * AWS Lambda handler responsible for orchestrating the news backfill process.
 * <p>
 * This handler triggers a pipeline that fetches enriched historical news data from S3 (JSON),
 * and indexes the results into OpenSearch.
 * It is optimized for high-throughput processing using Java Virtual Threads.
 * </p>
 * * <b>Key Features:</b>
 * <ul>
 * <li><b>Virtual Thread Executor:</b> Utilizes a structured task scope with a fixed pool
 * of virtual threads to maximize concurrency without pinning platform threads.</li>
 * <li><b>Flexible Ingestion:</b> Supports both partial backfills (after a specific date)
 * and full historical re-indexing.</li>
 * <li><b>Detailed Reporting:</b> Aggregates {@link IndexResult} to provide a summary
 * of success/failure counts and logs specific document errors.</li>
 * </ul>
 * * <b>Usage:</b>
 * The Lambda accepts a JSON input. For local testing:
 * <pre>
 * curl -X POST http://localhost:8080 \
 * -H "Content-Type: application/json" \
 * -d '{"afterDate": "2025-11-01"}'
 * </pre>
 */
@Slf4j
@ApplicationScoped
@Named("backfill-lambda")
public class BackfillIndexingLambdaHandler implements RequestHandler<BackfillLambdaInput, Integer> {

    @Inject
    private BackfillPipeline pipeline;

    @Override
    public Integer handleRequest(BackfillLambdaInput input, Context context) {
        try (var executor = Executors.newFixedThreadPool(200, Thread.ofVirtual().factory())) {

            if (!input.isSentinelDate()) {
                log.info("Starting backfill AFTER date: {}", input.getAfterDate());
            } else {
                log.info("Starting FULL backfill.");
            }
            long start = System.currentTimeMillis();
            List<IndexResult> results = pipeline.run(input.getAfterDate(), executor).join();
            long end = System.currentTimeMillis();

            long successCount = results.stream().filter(IndexResult::success).count();
            long failCount = results.size() - successCount;

            log.info("Job finished. Total: {}, Success: {}, Failed: {}, Took: {}", results.size(), successCount, failCount, end - start);

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

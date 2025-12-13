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
 *
 * curl usage:
 * curl -X POST http://localhost:8080 -H "Content-Type: application/json" -d '{"afterDate": "2025-11-01"}'
 */
@Slf4j
@ApplicationScoped
@Named("backfill-lambda")
public class BackfillIndexingLambdaHandler implements RequestHandler<BackfillLambdaInput, Integer> {

    @Inject
    private BackfillPipeline pipeline;

    @Override
    public Integer handleRequest(BackfillLambdaInput input, Context context) {
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {

            if (!input.isSentinelDate()) {
                log.info("Starting backfill AFTER date: {}", input.afterDate());
            } else {
                log.info("Starting FULL backfill.");
            }
            long start = System.currentTimeMillis();
            List<IndexResult> results = pipeline.run(input.afterDate(), executor).join();
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

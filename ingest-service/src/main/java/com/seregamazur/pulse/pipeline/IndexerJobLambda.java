package com.seregamazur.pulse.pipeline;

import java.util.List;
import java.util.concurrent.Executors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.seregamazur.pulse.indexing.model.IndexResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class IndexerJobLambda implements RequestHandler<Void, String> {

    @Inject
    Pipeline pipeline;

    @Override
    public String handleRequest(Void input, Context context) {
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            List<IndexResult> results = pipeline.run(executor).join();

            long successCount = results.stream().filter(IndexResult::success).count();
            long failCount = results.size() - successCount;

            log.info("Job finished. Total: {}, Success: {}, Failed: {}", results.size(), successCount, failCount);

            if (failCount > 0) {
                log.warn("Failures:");
                results.stream()
                    .filter(r -> !r.success())
                    .forEach(r -> log.warn("- {}: {}", r.title(), r.errorMessage()));
            }
        }
        return "0";
    }
}

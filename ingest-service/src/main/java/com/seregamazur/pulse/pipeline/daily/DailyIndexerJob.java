package com.seregamazur.pulse.pipeline.daily;

import java.util.List;
import java.util.concurrent.Executors;

import com.seregamazur.pulse.indexing.model.IndexResult;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@QuarkusMain
@ApplicationScoped
@Slf4j
public class DailyIndexerJob implements QuarkusApplication {
    @Inject
    private DailyPipeline pipeline;

    @Override
    public int run(String... args) {
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            long start = System.currentTimeMillis();
            List<IndexResult> results = pipeline.run(executor).join();
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

    public static void main(String... args) {
        Quarkus.run(DailyIndexerJob.class, args);
    }
}

package com.seregamazur.pulse.pipeline.historical;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@QuarkusMain(name = "process-csv-and-enrich-to-json")
@ApplicationScoped
@Slf4j
public class HistoricalDataEnrichmentJob implements QuarkusApplication {
    @Inject
    private HistoricalDataEnrichmentPipeline pipeline;

    @Override
    public int run(String... args) {
        pipeline.run().join();
        log.info("Job finished!");

        return 0;
    }

    public static void main(String... args) {
        Quarkus.run(HistoricalDataEnrichmentJob.class, args);
    }
}

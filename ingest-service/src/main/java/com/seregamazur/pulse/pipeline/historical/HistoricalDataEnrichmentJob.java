package com.seregamazur.pulse.pipeline.historical;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Local CLI tool for pre-processing and enriching historical news data.
 * <p>
 * <b>Purpose:</b> This is "Step 0" of the data lifecycle. It converts raw
 * legacy data (CSV) into the enriched format (JSON) required for the cloud Backfill process.
 * </p>
 * * <b>Operational Flow:</b>
 * 1. Read raw news data from local CSV files.
 * 2. Perform one-by-one enrichment via ChatGPT (Sequential/Small batches).
 * 3. Serialize the resulting {@code EnrichedNewsDocument} objects to local JSON files.
 * * <b>Context:</b>
 * - <b>Environment:</b> Strictly Local (runs as a standalone Java application).
 * - <b>Cost Management:</b> Processes items individually rather than in bulk
 * to optimize for token usage and error recovery during long-running tasks.
 * - <b>Output:</b> These local JSON files are intended to be manually uploaded
 * to S3 to serve as the source for the {@code BackfillIndexingLambdaHandler}.
 * * <b>Execution:</b>
 * Run via Maven/Gradle or as a Native executable with the specific name:
 * {@code quarkus run --name process-csv-and-enrich-to-json}
 */
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

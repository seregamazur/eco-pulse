package com.seregamazur.pulse.pipeline.daily;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.seregamazur.pulse.reading.model.RawNews;
import com.seregamazur.pulse.secret.SecretProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class GuardianService {

    private static final String SECTION = "environment";
    private static final String FIELDS = "body,headline";
    private static final String TAG = "-environment/series/country-diary";
    private static final int MAX_NEWS_COUNT_PER_DAY = 20;

    @Inject
    private SecretProvider secretProvider;

    @Inject
    private @RestClient TheGuardianClient client;

    public CompletableFuture<List<RawNews>> getDailyNews() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return client.search(
                SECTION,
                yesterday,
                yesterday,
                secretProvider.getGuardianApiKey(),
                MAX_NEWS_COUNT_PER_DAY,
                FIELDS,
                TAG
            )
            .toCompletableFuture()
            .thenApply(resp -> {
                    var results = resp.results();
                    if (results == null) return List.<RawNews>of();

                    return results.stream()
                        .filter(r -> r.headline() != null && r.body() != null)
                        .map(r -> new RawNews(
                            r.headline(),
                            HtmlCleaner.clean(r.body()),
                            r.webUrl(),
                            yesterday
                        ))
                        .toList();
                }
            ).exceptionally(e -> {
                log.error("Failed to process The Guardian response", e);
                return List.of();
            });
    }
}

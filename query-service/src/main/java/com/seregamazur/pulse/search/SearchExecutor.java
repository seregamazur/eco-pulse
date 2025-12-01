package com.seregamazur.pulse.search;

import java.io.IOException;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SearchExecutor {

    @Inject
    private OpenSearchClient client;

    public SearchResponse<JsonData> exec(SearchRequest request) {
        try {
            return client.search(request, JsonData.class);
        } catch (IOException e) {
            throw new OpenSearchResultException("OpenSearch search error", e);
        }
    }

    public <T> SearchResponse<T> exec(SearchRequest request, Class<T> clazz) {
        try {
            return client.search(request, clazz);
        } catch (IOException e) {
            throw new OpenSearchResultException("OpenSearch search error", e);
        }
    }

}

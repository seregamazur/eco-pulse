package com.seregamazur.pulse.search;

import java.io.IOException;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class SearchExecutor {

    @Inject
    @Named("openSearchClient")
    private OpenSearchClient client;

    public SearchResponse<ObjectNode> exec(SearchRequest request) {
        try {
            return client.search(request, ObjectNode.class);
        } catch (IOException e) {
            throw new OpenSearchResultException("OpenSearch search error", e);
        }
    }

}

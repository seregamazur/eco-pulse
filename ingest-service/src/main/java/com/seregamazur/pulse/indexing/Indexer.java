package com.seregamazur.pulse.indexing;

import java.io.IOException;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;

import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;
import com.seregamazur.pulse.indexing.model.IndexResult;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class Indexer {

    @Inject
    @Named("openSearchClient")
    private OpenSearchClient client;

    /**
     * Create index if not exists
     */
    @PostConstruct
    public void init() throws Exception {
        boolean exists = client.indices().exists(e -> e.index("news")).value();
        if (!exists) {
            CreateIndexRequest req = new CreateIndexRequest.Builder()
                .index("news")
                .settings(s -> s
                    .analysis(a -> a
                        .normalizer("lowercase_normalizer", n -> n
                            .custom(c -> c
                                .filter("lowercase")
                            )
                        )
                    )
                )
                .mappings(m -> m
                    .properties("title", p -> p.text(t -> t))
                    .properties("date", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
                    .properties("sentiment_label", p -> p.keyword(k -> k.normalizer("lowercase_normalizer")))
                    .properties("tone", p -> p.keyword(k -> k.normalizer("lowercase_normalizer")))
                    .properties("country", p -> p.keyword(k -> k))
                    .properties("sentiment_score", p -> p.float_(n -> n))
                    .properties("summary", p -> p.text(t -> t))
                    .properties("topics", p -> p.keyword(k -> k))
                    .properties("organizations", p -> p
                        .nested(n -> n
                            .properties("key", k -> k.keyword(kk -> kk.normalizer("lowercase_normalizer")))
                            .properties("raw", r -> r.keyword(kk -> kk))
                        )
                    )
                    .properties("keywords", p -> p
                        .nested(n -> n
                            .properties("key", k -> k.keyword(kk -> kk.normalizer("lowercase_normalizer")))
                            .properties("raw", r -> r.keyword(kk -> kk))
                        )
                    )
                    .properties("ingest_date", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
                )
                .build();
            client.indices().create(req);
        }
    }

    public void index(EnrichedNewsDocument doc) {
        IndexRequest<EnrichedNewsDocument> req = new IndexRequest.Builder<EnrichedNewsDocument>()
            .index("news")
            .document(doc)
            .id(DigestUtils.md5Hex(doc.getTitle()))
            .build();
        try {
            client.index(req);
            log.info("Successfully indexed document!");
        } catch (IOException e) {
            throw new RuntimeException("An exception occurred when trying to index document:" + doc.getTitle(), e);
        }
    }

    public List<IndexResult> bulkIndex(List<EnrichedNewsDocument> docs) {

        if (docs == null || docs.isEmpty()) return List.of();

        BulkRequest.Builder br = new BulkRequest.Builder();

        for (EnrichedNewsDocument doc : docs) {
            br.operations(op -> op
                .index(idx -> idx
                    .index("news")
                    .document(doc)
                    .id(DigestUtils.md5Hex(doc.getTitle()))
                )
            );
        }

        try {
            BulkResponse result = client.bulk(br.build());

            if (result.errors()) {
                result.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error("Bulk item failed: id={}, reason={}", item.id(), item.error().reason());
                    }
                });
            }

            return result.items().stream()
                .map(item -> new IndexResult(
                    item.id(),
                    item.error() == null,
                    item.error() != null ? item.error().reason() : null
                ))
                .toList();

        } catch (IOException e) {
            log.error("Critical Bulk Indexing Failure: {}", e.getMessage());
            return docs.stream()
                .map(d -> IndexResult.fail(d.getTitle(), e))
                .toList();
        }
    }
}



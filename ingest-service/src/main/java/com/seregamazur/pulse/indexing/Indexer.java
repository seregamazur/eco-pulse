package com.seregamazur.pulse.indexing;

import java.io.IOException;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;

import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class Indexer {

    @Inject
    OpenSearchClient client;

    public Indexer(OpenSearchClient osClient) {
        this.client = osClient;
    }

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
                    .properties("topics", p -> p
                        .nested(n -> n
                            .properties("key", k -> k.keyword(kk -> kk.normalizer("lowercase_normalizer")))
                            .properties("raw", r -> r.keyword(kk -> kk))
                        )
                    )
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
            .build();
        try {
            client.index(req);
            log.info("Successfully indexed document!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}


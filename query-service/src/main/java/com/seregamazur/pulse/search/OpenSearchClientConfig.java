package com.seregamazur.pulse.search;

import org.apache.hc.core5.http.HttpHost;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class OpenSearchClientConfig {

    @Inject
    @ConfigProperty(name = "quarkus.opensearch.hosts")
    private String host;

    @Inject
    @ConfigProperty(name = "quarkus.opensearch.protocol")
    private String protocol;

    @Inject
    @ConfigProperty(name = "quarkus.opensearch.port")
    private int port;

    @Produces
    public OpenSearchClient openSearchClient() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        var jsonpMapper = new JacksonJsonpMapper(mapper);

        var transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost(protocol, host, port))
            .setMapper(jsonpMapper)
            .build();

        return new OpenSearchClient(transport);
    }
}

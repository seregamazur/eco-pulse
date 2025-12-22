package com.seregamazur.pulse.search;

import java.time.Duration;

import org.apache.hc.core5.http.HttpHost;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;

/**
 * Provide OpenSearch client for local and prod profiles
 */
@ApplicationScoped
public class OpenSearchClientProvider {

    @Inject
    @ConfigProperty(name = "opensearch.hosts")
    private Provider<String> host;

    @Inject
    @ConfigProperty(name = "opensearch.port")
    private int port;

    @Inject
    @ConfigProperty(name = "opensearch.aws.region")
    private String region;

    @Inject
    @ConfigProperty(name = "opensearch.aws.service")
    private String signService;

    @Inject
    @ConfigProperty(name = "opensearch.max-concurrency")
    private int maxConcurrency;

    @Inject
    @ConfigProperty(name = "opensearch.connection-acquisition-timeout")
    private Duration timeout;

    @Inject
    @ConfigProperty(name = "aws.enabled")
    private boolean awsEnabled;

    @Produces
    @Singleton
    @Named("openSearchClient")
    @IfBuildProfile("local")
    public OpenSearchClient localOpenSearchClient() {
        if (!awsEnabled) {
            var jsonpMapper = new JacksonJsonpMapper(objectMapper());

            OpenSearchTransport transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost("http", host.get(), port))
                .setMapper(jsonpMapper)
                .build();

            return new OpenSearchClient(transport);
        }
        throw new ConfigurationException("AWS flag should be set to false for local env!");
    }

    /**
     * Async Non-blocking Netty client for concurrency
     * AWS SigV4 sign
     *
     * @return Configured {@link OpenSearchClient} for prod AWS
     */
    @Produces
    @Singleton
    @Named("openSearchClient")
    @IfBuildProfile("prod")
    public OpenSearchClient prodOpenSearchClient() {
        if (awsEnabled) {
            SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .connectionAcquisitionTimeout(timeout)
                .maxConcurrency(maxConcurrency)
                .build();

            JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper());

            OpenSearchTransport transport = new AwsSdk2Transport(
                httpClient,
                host.get(),
                signService,
                Region.of(region),
                AwsSdk2TransportOptions.builder()
                    .setMapper(jsonpMapper)
                    .build()
            );

            return new OpenSearchClient(transport);
        }
        throw new ConfigurationException("AWS flag should be set to true for prod env!");
    }

    @Produces
    @Singleton
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        mapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        return mapper;
    }
}

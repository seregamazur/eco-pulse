package com.seregamazur.pulse.indexing.client;

import java.time.Duration;

import org.apache.hc.core5.http.HttpHost;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
    private String host;

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

    @Inject
    private ObjectMapper objectMapper;

    @Produces
    @Singleton
    @Named("openSearchClient")
    @IfBuildProfile("local")
    public OpenSearchClient localOpenSearchClient() {
        if (!awsEnabled) {
            var jsonpMapper = new JacksonJsonpMapper(objectMapper);

            OpenSearchTransport transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost("http", host, port))
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

            JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);

            OpenSearchTransport transport = new AwsSdk2Transport(
                httpClient,
                host,
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
}

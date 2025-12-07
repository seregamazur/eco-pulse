package com.seregamazur.pulse.reading.s3;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

@ApplicationScoped
@Slf4j
public class S3FileService {

    @Inject
    private S3AsyncClient s3;

    @ConfigProperty(name = "bucket.name")
    String bucketName;

    public CompletableFuture<List<String>> listCsvFiles() {
        var s3Request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .build();
        S3AsyncObjectsCollector collector = new S3AsyncObjectsCollector();
        s3.listObjectsV2Paginator(s3Request).subscribe(collector);

        return collector.future()
            .thenApply(this::filterCsv).exceptionally(e -> {
                log.error("Failed to get list of csv files from S3", e);
                return null;
            });
    }

    public CompletableFuture<ResponseBytes<GetObjectResponse>> downloadFile(String key) {
        return s3.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(key).build(),
            AsyncResponseTransformer.toBytes());
    }

    private List<String> filterCsv(List<S3Object> s3Objects) {
        return s3Objects.stream()
            .map(S3Object::key)
            .filter(o -> o.endsWith(".csv"))
            .toList();
    }

}

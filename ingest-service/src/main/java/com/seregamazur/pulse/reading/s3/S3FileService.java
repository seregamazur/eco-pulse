package com.seregamazur.pulse.reading.s3;

import java.time.LocalDate;
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

    @Inject
    private S3KeyUtils s3KeyUtils;

    @ConfigProperty(name = "bucket.name")
    private String bucketName;

    @ConfigProperty(name = "bucket.prefix.raw-news")
    private String bucketPrefixRawNews;

    @ConfigProperty(name = "bucket.prefix.enriched-news")
    private String bucketPrefixEnrichedNews;

    public CompletableFuture<List<String>> listRawNewsFiles() {
        return listFilesByPrefix(bucketPrefixRawNews);
    }

    public CompletableFuture<List<String>> listEnrichedNewsFiles(LocalDate after) {
        return listFilesByPrefix(bucketPrefixEnrichedNews)
            .thenApply(l -> l.stream()
                .filter(key -> LocalDate.parse(s3KeyUtils.getBaseNameFromS3Key(key)).isAfter(after))
                .toList());
    }

    private CompletableFuture<List<String>> listFilesByPrefix(String bucketPrefix) {
        var s3Request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(bucketPrefix)
            .build();
        S3AsyncObjectsCollector collector = new S3AsyncObjectsCollector();
        s3.listObjectsV2Paginator(s3Request).subscribe(collector);

        return collector.future().thenApply(f -> f.stream().map(S3Object::key).toList());
    }

    public CompletableFuture<ResponseBytes<GetObjectResponse>> downloadFile(String key) {
        return s3.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(key).build(),
            AsyncResponseTransformer.toBytes());
    }

}

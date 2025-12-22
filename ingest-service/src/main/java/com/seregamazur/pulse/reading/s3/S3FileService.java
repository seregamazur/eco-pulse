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

/**
 * Service for interacting with AWS S3 to manage news files.
 * <p>
 * This service handles non-blocking I/O operations for listing and downloading
 * news documents. It distinguishes between "raw" news (initial ingestion)
 * and "enriched" news (processed by AI).
 * </p>
 */
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

    /**
     * Lists enriched news files, filtering them based on a target date.
     * <p>
     * <b>Filtering Logic:</b>
     * To support incremental backfills, this method excludes:
     * <ul>
     * <li>S3 Folder markers (keys ending in /).</li>
     * <li>System files or metadata (non-date-formatted keys).</li>
     * <li>Files representing dates equal to or before the 'after' parameter.</li>
     * </ul>
     * </p>
     * @param after The threshold date; only files representing dates strictly after this are included.
     * @return A list of S3 keys representing relevant JSON files.
     */
    public CompletableFuture<List<String>> listEnrichedNewsFiles(LocalDate after) {
        return listFilesByPrefix(bucketPrefixEnrichedNews)
            .thenApply(l -> l.stream()
                .filter(key -> {
                    String baseName = s3KeyUtils.getBaseNameFromS3Key(key);
                    // filter out folder or other
                    if (baseName == null || !baseName.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return false;
                    }
                    return LocalDate.parse(baseName).isAfter(after);
                })
                .toList());
    }

    /**
     * Lists all objects under a specific S3 prefix using a reactive paginator.
     * <p>
     * <b>Note:</b> S3 is a flat storage; "prefixes" simulate a directory structure.
     * This method subscribes to a paginated stream to handle buckets with thousands of files.
     * </p>
     */
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

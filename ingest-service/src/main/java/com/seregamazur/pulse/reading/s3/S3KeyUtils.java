package com.seregamazur.pulse.reading.s3;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class S3KeyUtils {

    @ConfigProperty(name = "bucket.prefix.raw-news")
    private String bucketPrefixRawNews;
    @ConfigProperty(name = "bucket.prefix.enriched-news")
    private String bucketPrefixEnrichedNews;
    private static final String CSV_EXTENSION = ".csv";
    private static final String JSON_EXTENSION = ".json";

    public String removePrefix(String fullKey) {

        if (fullKey == null) {
            return null;
        }

        if (fullKey.startsWith(bucketPrefixRawNews + "/")) {
            return fullKey.substring(bucketPrefixRawNews.length() + 1);
        } else if (fullKey.startsWith(bucketPrefixEnrichedNews + "/")) {
            return fullKey.substring(bucketPrefixEnrichedNews.length() + 1);
        }

        return fullKey;
    }

    public String removeExtension(String fileName) {
        if (fileName != null && fileName.endsWith(CSV_EXTENSION)) {
            return fileName.substring(0, fileName.length() - CSV_EXTENSION.length());
        } else if (fileName != null && fileName.endsWith(JSON_EXTENSION)) {
            return fileName.substring(0, fileName.length() - JSON_EXTENSION.length());
        }
        return fileName;
    }

    public String removePrefixAndExtension(String fullKey) {
        String fileName = removePrefix(fullKey);
        return removeExtension(fileName);
    }

    public String getBaseNameFromS3Key(String s3Key) {
        return removePrefixAndExtension(s3Key);
    }
}


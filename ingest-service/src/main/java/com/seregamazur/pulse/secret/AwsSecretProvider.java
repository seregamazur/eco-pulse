package com.seregamazur.pulse.secret;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@IfBuildProfile("prod")
@Singleton
@Slf4j
public class AwsSecretProvider implements SecretProvider {

    @ConfigProperty(name = "secrets.manager.name")
    String secretName;

    @Inject
    @ConfigProperty(name = "aws.region")
    private String awsRegion;

    @Inject
    ObjectMapper objectMapper;

    private String guardianApiKey;
    private String gptApiKey;

    @PostConstruct
    void loadSecrets() {
        SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

            String secretString = secretsManagerClient.getSecretValue(valueRequest).secretString();

            if (secretString == null || secretString.isBlank()) {
                throw new RuntimeException("SECRET_ERROR: Secret string is null or empty. Check IAM permissions and Secret ID.");
            }

            Map<String, String> secretMap;
            try {
                secretMap = objectMapper.readValue(secretString, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                log.error("JACKSON FAIL on SecretManager: {}", e.getMessage());
                log.error("Secret content (first 5 chars): {}", secretString.substring(0, Math.min(secretString.length(), 5)));

                secretMap = new HashMap<>();
                secretMap.put("OPENSEARCH_HOST", secretString.trim());
            }

            this.guardianApiKey = secretMap.get("GUARDIAN_API_KEY");
            this.gptApiKey = secretMap.get("GPT_KEY");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load secrets from AWS Secrets Manager: " + secretName, e);
        }
    }

    @Override
    public String getGuardianApiKey() {
        return guardianApiKey;
    }

    @Override
    public String getGptApiKey() {
        return gptApiKey;
    }
}
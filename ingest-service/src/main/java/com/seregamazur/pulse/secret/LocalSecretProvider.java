package com.seregamazur.pulse.secret;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@IfBuildProfile("local")
public class LocalSecretProvider implements SecretProvider {

    @Inject
    @ConfigProperty(name = "guardian.key")
    private String guardianApikey;

    @Inject
    @ConfigProperty(name = "gpt.key")
    private String gptApiKey;

    @Override
    public String getGuardianApiKey() {
        return guardianApikey;
    }

    @Override
    public String getGptApiKey() {
        return gptApiKey;
    }
}

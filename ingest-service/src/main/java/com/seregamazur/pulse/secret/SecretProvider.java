package com.seregamazur.pulse.secret;

public interface SecretProvider {

    String getGuardianApiKey();

    String getGptApiKey();
}
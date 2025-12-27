package com.seregamazur.pulse.pipeline.daily.dto;

public record TheGuardianRawNews(
    String headline,
    String body,
    String webUrl
) {
}

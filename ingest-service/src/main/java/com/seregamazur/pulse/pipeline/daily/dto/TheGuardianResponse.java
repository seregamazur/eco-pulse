package com.seregamazur.pulse.pipeline.daily.dto;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.seregamazur.pulse.pipeline.daily.GuardianResponseDeserializer;

@JsonDeserialize(using = GuardianResponseDeserializer.class)
public record TheGuardianResponse(
    List<TheGuardianRawNews> results
) {
}

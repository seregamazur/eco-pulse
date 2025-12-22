package com.seregamazur.pulse.pipeline.backfill;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;

@Getter
@RegisterForReflection
public final class BackfillLambdaInput {
    private static final LocalDate SENTINEL_DATE = LocalDate.of(1970, 1, 1);

    private final LocalDate afterDate;

    //This is String due to Quarkus Native settings for ObjectMapper
    @JsonCreator
    public BackfillLambdaInput(@JsonProperty("afterDate") String afterDate) {
        if (afterDate == null || afterDate.isBlank()) {
            this.afterDate = SENTINEL_DATE;
        } else {
            this.afterDate = LocalDate.parse(afterDate);
        }
    }

    public boolean isSentinelDate() {
        return afterDate == null;
    }
}

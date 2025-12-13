package com.seregamazur.pulse.pipeline.backfill;

import java.time.LocalDate;

public record BackfillLambdaInput(LocalDate afterDate) {

    private static final LocalDate SENTINEL_DATE = LocalDate.of(1970, 1, 1);

    @Override
    public LocalDate afterDate() {
        return afterDate == null ? SENTINEL_DATE : afterDate;
    }

    public boolean isSentinelDate() {
        return afterDate == null;
    }
}

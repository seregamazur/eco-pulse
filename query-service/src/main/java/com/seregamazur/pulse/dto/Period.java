package com.seregamazur.pulse.dto;

import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum Period {
    DAY(CalendarInterval.Day),
    WEEK(CalendarInterval.Week),
    MONTH(CalendarInterval.Month),
    QUARTER(CalendarInterval.Quarter),
    YEAR(CalendarInterval.Year);

    private final CalendarInterval interval;

    Period(CalendarInterval interval) {
        this.interval = interval;
    }

    public CalendarInterval interval() {
        return interval;
    }
}


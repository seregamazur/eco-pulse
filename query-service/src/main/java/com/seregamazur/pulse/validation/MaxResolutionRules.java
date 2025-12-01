package com.seregamazur.pulse.validation;

import java.util.Map;

import com.seregamazur.pulse.dto.Period;

public final class MaxResolutionRules {

    public static final Map<Period, Long> MAX_DAYS = Map.of(
        Period.DAY, 183L,         // half of a year in days
        Period.WEEK, 365L * 2,    // 2 years in weeks
        Period.MONTH, 365L * 5,   // 5 years in months
        Period.QUARTER, 365L * 5, // 5 years in quarters
        Period.YEAR, 365L * 5         // 5 years
    );
}

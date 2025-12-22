package com.seregamazur.pulse.dto.sentiment;

import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AverageSentiment(LocalDate date,
                               double avgScore,
                               long positive,
                               long negative,
                               long neutral) {
}

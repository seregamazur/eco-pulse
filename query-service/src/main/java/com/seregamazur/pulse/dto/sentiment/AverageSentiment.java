package com.seregamazur.pulse.dto.sentiment;

import java.time.LocalDate;

public record AverageSentiment(LocalDate date,
                               double avgScore,
                               long positive,
                               long negative,
                               long neutral) {
}

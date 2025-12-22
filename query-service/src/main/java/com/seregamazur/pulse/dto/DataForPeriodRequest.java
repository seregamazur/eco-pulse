package com.seregamazur.pulse.dto;

import java.time.LocalDate;

import com.seregamazur.pulse.validation.ValidDateRequest;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.QueryParam;

@ValidDateRequest
@RegisterForReflection
public record DataForPeriodRequest(@QueryParam("from") LocalDate from,
                                   @QueryParam("to") LocalDate to, @QueryParam("period") Period period) {
}


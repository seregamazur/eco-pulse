package com.seregamazur.pulse.exception;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ErrorDto(String message) {
}

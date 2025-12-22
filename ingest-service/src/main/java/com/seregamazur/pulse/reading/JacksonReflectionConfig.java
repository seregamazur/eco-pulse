package com.seregamazur.pulse.reading;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Force GraalVM to keep Jackson modules in the native binary.
 * Without this, GraalVM prunes them during the "Daily" profile build.
 */
@RegisterForReflection(
    targets = {
        com.fasterxml.jackson.datatype.jdk8.Jdk8Module.class,
        com.fasterxml.jackson.datatype.jsr310.JavaTimeModule.class,
        com.fasterxml.jackson.module.paramnames.ParameterNamesModule.class,
        com.fasterxml.jackson.datatype.joda.JodaModule.class
    }
)
public class JacksonReflectionConfig {
}

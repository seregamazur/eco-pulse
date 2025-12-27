package com.seregamazur.pulse.reading.model;

import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record RawNews(String title, String text, String url, LocalDate date) {
}

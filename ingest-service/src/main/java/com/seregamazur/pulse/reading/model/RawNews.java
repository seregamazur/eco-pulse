package com.seregamazur.pulse.reading.model;

import java.time.LocalDate;

public record RawNews(String title, String author, String text, LocalDate date) {
}

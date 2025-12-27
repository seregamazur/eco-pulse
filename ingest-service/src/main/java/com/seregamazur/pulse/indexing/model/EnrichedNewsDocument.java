package com.seregamazur.pulse.indexing.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@RegisterForReflection
public class EnrichedNewsDocument {
    private String title;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;
    @JsonProperty("sentiment_label")
    private SentimentLabel sentimentLabel;
    @JsonProperty("sentiment_score")
    private float sentimentScore;
    private Tone tone;
    private List<Topic> topics;
    private String country;
    private List<KeywordValue> organizations;
    private List<KeywordValue> keywords;
    private String summary;
    @JsonProperty("web_url")
    private String webUrl;
    @JsonProperty("ingest_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate ingestDate;
}

@RegisterForReflection
enum Tone {
    SCIENTIFIC,
    ACTIVIST,
    DESCRIPTIVE
}

@RegisterForReflection
enum SentimentLabel {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

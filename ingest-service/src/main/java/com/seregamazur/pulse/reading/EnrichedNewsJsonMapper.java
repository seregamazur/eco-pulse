package com.seregamazur.pulse.reading;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.experimental.UtilityClass;

@ApplicationScoped
@UtilityClass
public final class EnrichedNewsJsonMapper {

    @Inject
    private ObjectMapper mapper;

    public static List<EnrichedNewsDocument> readEnrichedNews(byte[] bytes) {
        try {
            return mapper.readValue(bytes, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("An exception occurred when trying to map EnrichedNews from JSON!", e);
        }
    }

    public static void writeEnrichedToFile(List<EnrichedNewsDocument> doc) {
        try {
            mapper.writeValue(new File("/data/enriched/" + doc.getFirst().getDate().toString() + ".json"), doc);
        } catch (IOException e) {
            throw new RuntimeException("An exception occurred when trying to save EnrichedNews to JSON!", e);
        }
    }
}

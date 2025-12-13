package com.seregamazur.pulse.reading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public final class EnrichedNewsJsonMapper {

    @Inject
    private ObjectMapper mapper;

    public List<EnrichedNewsDocument> readEnrichedNews(byte[] bytes) {
        try {
            return mapper.readValue(bytes, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("An exception occurred when trying to map EnrichedNews from JSON!", e);
        }
    }

    public void writeEnrichedToFile(List<EnrichedNewsDocument> doc) {

        Path outputDir = Path.of("data", "enriched-final");
        String fileName = doc.getFirst().getDate().toString() + ".json";
        Path filePath = outputDir.resolve(fileName);

        try {
            Files.createDirectories(filePath.getParent());

            mapper.writeValue(filePath.toFile(), doc);
        } catch (IOException e) {
            throw new RuntimeException("An exception occurred when trying to write enriched data to file!", e);
        }
    }
}

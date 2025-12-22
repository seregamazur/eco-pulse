package com.seregamazur.pulse.reading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seregamazur.pulse.indexing.model.EnrichedNewsDocument;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public final class EnrichedNewsJsonMapper {

    public List<EnrichedNewsDocument> readEnrichedNews(byte[] bytes) {
        try {
            EnrichedNewsDocument[] docs = objectMapper().readValue(bytes, EnrichedNewsDocument[].class);
            return Arrays.asList(docs);
        } catch (IOException e) {
            log.error("Error in reading enriched news");
            throw new RuntimeException("An exception occurred when trying to map EnrichedNews from JSON!", e);
        }
    }

    public void writeEnrichedToFile(List<EnrichedNewsDocument> doc) {

        Path outputDir = Path.of("data", "enriched-final");
        String fileName = doc.getFirst().getDate().toString() + ".json";
        Path filePath = outputDir.resolve(fileName);

        try {
            Files.createDirectories(filePath.getParent());

            objectMapper().writeValue(filePath.toFile(), doc);
        } catch (IOException e) {
            throw new RuntimeException("An exception occurred when trying to write enriched data to file!", e);
        }
    }

    @Produces
    @Singleton
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        mapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        return mapper;
    }
}

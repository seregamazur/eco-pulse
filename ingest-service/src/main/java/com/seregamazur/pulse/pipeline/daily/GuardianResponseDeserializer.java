package com.seregamazur.pulse.pipeline.daily;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.seregamazur.pulse.pipeline.daily.dto.TheGuardianRawNews;
import com.seregamazur.pulse.pipeline.daily.dto.TheGuardianResponse;

public class GuardianResponseDeserializer extends JsonDeserializer<TheGuardianResponse> {

    @Override
    public TheGuardianResponse deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode root = p.getCodec().readTree(p);

        JsonNode response = root.get("response");
        if (response == null) return new TheGuardianResponse(List.of());

        JsonNode resultsNode = response.get("results");
        if (resultsNode == null || !resultsNode.isArray()) return new TheGuardianResponse(List.of());

        List<TheGuardianRawNews> list = new ArrayList<>();

        for (JsonNode item : resultsNode) {
            JsonNode fields = item.get("fields");

            String headline = fields != null && fields.has("headline")
                ? fields.get("headline").asText()
                : null;

            String body = fields != null && fields.has("body")
                ? fields.get("body").asText()
                : null;

            list.add(new TheGuardianRawNews(headline, body));
        }

        return new TheGuardianResponse(list);
    }
}

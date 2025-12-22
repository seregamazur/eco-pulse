package com.seregamazur.pulse.dto.geo;

import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record GeoMapChart(Map<String, CountryNewsData> countryNews) {
}

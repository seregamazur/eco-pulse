package com.seregamazur.pulse.dto.geo;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CountryNewsData(long newsCount,
                              long positive,
                              long negative,
                              long neutral,
                              double avgSentiment) {
}

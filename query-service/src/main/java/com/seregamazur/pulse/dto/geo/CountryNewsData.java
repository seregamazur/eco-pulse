package com.seregamazur.pulse.dto.geo;

public record CountryNewsData(long newsCount,
                              long positive,
                              long negative,
                              long neutral,
                              double avgSentiment) {
}

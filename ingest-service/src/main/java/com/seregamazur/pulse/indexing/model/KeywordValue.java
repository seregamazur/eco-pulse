package com.seregamazur.pulse.indexing.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KeywordValue {
    private String indexKey;
    private String rawKey;

    public KeywordValue(String rawKey) {
        this.rawKey = rawKey;
        this.indexKey = rawKey;
    }

}

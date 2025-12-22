package com.seregamazur.pulse.indexing.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@RegisterForReflection
public class KeywordValue {
    private String key;
    private String raw;

    public KeywordValue(String rawKey) {
        this.raw = rawKey;
        this.key = rawKey;
    }

}

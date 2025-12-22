package com.seregamazur.pulse.dto.topics;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TopicsOverTime(String name, long count) {
}

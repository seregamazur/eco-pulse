package com.seregamazur.pulse.dto.topics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TopicsChart(Map<LocalDate, List<TopicsOverTime>> topics) {
}

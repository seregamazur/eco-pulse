package com.seregamazur.pulse.dto.topics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TopicsChart(Map<LocalDate, List<TopicsOverTime>> topics) {
}

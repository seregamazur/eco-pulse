package com.seregamazur.pulse.dto.today;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TodayNews(String title, String summary, String label, String webUrl) {
}

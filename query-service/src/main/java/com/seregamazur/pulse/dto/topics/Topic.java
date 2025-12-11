package com.seregamazur.pulse.dto.topics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@Getter
public enum Topic {
    CLIMATE_CHANGE("Climate Change"),
    RENEWABLE_ENERGY("Renewable Energy"),
    FOSSIL_FUELS("Fossil Fuels"),
    AIR_POLLUTION("Air Pollution"),
    WATER_POLLUTION("Water Pollution"),
    BIODIVERSITY_LAND("Biodiversity Land"),
    WASTE_RECYCLING("Waste Recycling"),
    EXTREME_EVENTS("Extreme Events"),
    SOCIAL_RISKS("Social Risks"),
    PUBLIC_HEALTH("Public Health"),
    FINES_SANCTIONS("Fines Sanctions"),
    OTHER("Other");

    private final String raw;

    Topic(String raw) {
        this.raw = raw;
    }

    /*
    Even though our prompt as strict as possible, model can return something not from the enum
     */
    @JsonCreator
    public static Topic fromString(String value) {
        for (Topic t : Topic.values()) {
            if (t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        return OTHER;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

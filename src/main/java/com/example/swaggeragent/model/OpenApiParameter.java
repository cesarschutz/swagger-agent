package com.example.swaggeragent.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenApiParameter(
        String name,
        String in,
        String description,
        boolean required,
        String type,
        String format,
        Object defaultValue,
        List<String> enumValues,
        OpenApiParameterItems items
) {
} 
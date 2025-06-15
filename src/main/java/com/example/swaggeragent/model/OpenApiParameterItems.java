package com.example.swaggeragent.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenApiParameterItems(
    String type,
    String format
) {
} 
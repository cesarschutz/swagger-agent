package com.example.swaggeragent.model;

import io.swagger.v3.oas.models.media.Schema;

public record OpenApiMediaType(
        Schema schema,
        Object example
) {
} 
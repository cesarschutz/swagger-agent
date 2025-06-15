package com.example.swaggeragent.model;

import java.util.Map;

public record OpenApiResponse(
        String description,
        Map<String, OpenApiMediaType> content
) {
} 
package com.example.springialocal.domain.tool.model;

import java.util.Map;

public record OpenApiResponse (
    String description,
    Map<String, OpenApiMediaType> content
) { }

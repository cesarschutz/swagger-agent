package com.example.springialocal.domain.tool.model;

import java.util.Map;

public record OpenApiRequestBody (
    String description,
    boolean required,
    Map<String, OpenApiMediaType> content
) { }

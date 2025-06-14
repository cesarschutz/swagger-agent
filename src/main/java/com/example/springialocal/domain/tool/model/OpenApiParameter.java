package com.example.springialocal.domain.tool.model;

import java.util.List;

public record OpenApiParameter (
    String name,
    String in,
    String description,
    boolean required,
    String type,
    String format,
    Object defaultValue,
    List<String> enumValues
){}
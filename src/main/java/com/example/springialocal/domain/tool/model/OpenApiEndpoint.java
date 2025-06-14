package com.example.springialocal.domain.tool.model;

import java.util.List;
import java.util.Map;

public record OpenApiEndpoint (
    String operationId,
    String method,
    String path,
    String summary,
    String description,
    String baseUrl,
    List<OpenApiParameter> parameters,
    OpenApiRequestBody requestBody,
    Map<String, OpenApiResponse> responses,
    List<String> tags
) { }


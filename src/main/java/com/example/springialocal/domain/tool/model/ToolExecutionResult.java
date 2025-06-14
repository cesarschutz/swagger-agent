package com.example.springialocal.domain.tool.model;

/**
 * Represents the result of a tool execution.
 *
 * @param httpStatusCode The HTTP status code of the API call.
 * @param body           The response body from the API call.
 */
public record ToolExecutionResult(int httpStatusCode, String body) {
} 
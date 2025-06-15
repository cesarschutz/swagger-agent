package com.example.springialocal.service;

import com.example.springialocal.model.OpenApiEndpoint;
import com.example.springialocal.model.OpenApiParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for generating JSON schemas from OpenAPI endpoint definitions.
 */
@Service
public class JsonSchemaGenerator {

    private final ObjectMapper objectMapper;

    public JsonSchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a JSON schema for a given OpenAPI endpoint.
     *
     * @param endpoint the OpenAPI endpoint
     * @return a JSON string representing the schema
     */
    public String generateJsonSchema(OpenApiEndpoint endpoint) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        List<String> required = new ArrayList<>();

        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                ObjectNode paramSchema = objectMapper.createObjectNode();
                paramSchema.put("type", mapOpenApiTypeToJsonSchemaType(param.type()));
                paramSchema.put("description", param.description() != null ? param.description() : "");

                if (param.enumValues() != null && !param.enumValues().isEmpty()) {
                    paramSchema.set("enum", objectMapper.valueToTree(param.enumValues()));
                }

                if (param.defaultValue() != null) {
                    paramSchema.set("default", objectMapper.valueToTree(param.defaultValue()));
                }

                properties.set(param.name(), paramSchema);

                if (param.required()) {
                    required.add(param.name());
                }
            }
        }

        if (endpoint.requestBody() != null) {
            ObjectNode bodySchema = objectMapper.createObjectNode();
            bodySchema.put("type", "object");
            bodySchema.put("description", "Request body data");
            properties.set("requestBody", bodySchema);

            if (endpoint.requestBody().required()) {
                required.add("requestBody");
            }
        }

        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.set("required", objectMapper.valueToTree(required));
        }

        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            // Log the exception? Or rethrow a custom one?
            // For now, returning an empty schema for simplicity, but this could be improved.
            return "{}";
        }
    }

    private String mapOpenApiTypeToJsonSchemaType(String openApiType) {
        if (openApiType == null) {
            return "string";
        }
        return switch (openApiType.toLowerCase()) {
            case "integer", "int32", "int64" -> "integer";
            case "number", "float", "double" -> "number";
            case "boolean" -> "boolean";
            case "array" -> "array";
            case "object" -> "object";
            default -> "string";
        };
    }
} 
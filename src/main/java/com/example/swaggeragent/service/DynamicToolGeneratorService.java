package com.example.swaggeragent.service;

import com.example.swaggeragent.model.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiParameter;
import com.example.swaggeragent.model.ToolExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class DynamicToolGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolGeneratorService.class);

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final boolean toolLoggingEnabled;

    public DynamicToolGeneratorService(
        ObjectMapper objectMapper,
        WebClient.Builder webClientBuilder,
        @Value("${app.tool.logging.enabled:false}") boolean toolLoggingEnabled) {
            this.objectMapper = objectMapper;
            this.webClientBuilder = webClientBuilder;
            this.toolLoggingEnabled = toolLoggingEnabled;
    }

    public List<DynamicTool> generateToolsFromEndpoints(List<OpenApiEndpoint> endpoints) {
        List<DynamicTool> tools = new ArrayList<>();
        List<String> usedNames = new ArrayList<>();

        for (OpenApiEndpoint endpoint : endpoints) {
            try {
                String toolName = generateToolName(endpoint, usedNames);
                usedNames.add(toolName);

                DynamicTool tool = generateToolFromEndpoint(endpoint, toolName);
                tools.add(tool);
            } catch (Exception e) {
                if (toolLoggingEnabled) {
                    log.error("Error generating tool for endpoint: {} {}", endpoint.method(), endpoint.path(), e);
                }
            }
        }
        return tools;
    }

    private DynamicTool generateToolFromEndpoint(OpenApiEndpoint endpoint, String toolName) {
        String description = generateToolDescription(endpoint);
        String jsonSchema = generateJsonSchema(endpoint);
        Function<Object, String> function = generateFunction(endpoint);

        return DynamicTool.builder()
                .name(toolName)
                .description(description)
                .endpoint(endpoint)
                .function(function)
                .jsonSchema(jsonSchema)
                .build();
    }

    private String generateToolName(OpenApiEndpoint endpoint, List<String> usedNames) {
        String controllerName = "general";
        if (endpoint.tags() != null && !endpoint.tags().isEmpty()) {
            controllerName = endpoint.tags().get(0).replace("-controller", "");
        }

        String operationName = endpoint.operationId() != null && !endpoint.operationId().isEmpty()
                ? endpoint.operationId()
                : endpoint.method().toLowerCase() + "_" + endpoint.path().replaceAll("[^a-zA-Z0-9]", "_");

        String baseName = controllerName + "." + operationName;

        String toolName = baseName;
        int counter = 1;
        while (usedNames.contains(toolName)) {
            toolName = baseName + "_" + counter++;
        }
        return toolName;
    }

    private String generateToolDescription(OpenApiEndpoint endpoint) {
        StringBuilder description = new StringBuilder();

        if (endpoint.summary() != null && !endpoint.summary().isEmpty()) {
            description.append(endpoint.summary());
        } else if (endpoint.description() != null && !endpoint.description().isEmpty()) {
            description.append(endpoint.description());
        } else {
            description.append("Executes a ").append(endpoint.method().toUpperCase())
                    .append(" request to the path '").append(endpoint.path()).append("'.");
        }

        if (!description.toString().endsWith(".")) {
            description.append(".");
        }

        description.append(" This tool is used to perform an operation related to '").append(String.join(", ", endpoint.tags())).append("'.");

        if (endpoint.parameters() != null && !endpoint.parameters().isEmpty()) {
            description.append(" It requires the following parameters: ");
            List<String> paramDetails = endpoint.parameters().stream()
                    .map(p -> String.format("%s in %s (%s, %s)", p.name(), p.in(), p.type(), p.required() ? "required" : "optional"))
                    .toList();
            description.append(String.join("; ", paramDetails)).append(".");
        }

        if (endpoint.requestBody() != null && endpoint.requestBody().required()) {
            description.append(" A request body is required.");
        }

        endpoint.responses().forEach((statusCode, response) -> {
            if (statusCode.startsWith("2")) { // Success responses
                description.append(" On success (HTTP ").append(statusCode).append("), it returns ");
                if (response.content() != null && !response.content().isEmpty()) {
                    String mediaType = response.content().keySet().iterator().next();
                    String schemaRef = response.content().get(mediaType).schema().get$ref();
                    if (schemaRef != null) {
                        description.append("a '").append(schemaRef.substring(schemaRef.lastIndexOf('/') + 1)).append("' object.");
                    } else {
                        description.append("a response of type '").append(mediaType).append("'.");
                    }
                } else {
                    description.append("no content.");
                }
            }
        });

        return description.toString();
    }

    private String generateJsonSchema(OpenApiEndpoint endpoint) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        List<String> required = new ArrayList<>();

        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                ObjectNode paramSchema = objectMapper.createObjectNode();
                paramSchema.put("type", mapOpenApiTypeToJsonSchemaType(param.type()));
                paramSchema.put("description", param.description() != null ? param.description() : "");

                if ("array".equals(param.type()) && param.items() != null) {
                    ObjectNode itemsSchema = objectMapper.createObjectNode();
                    itemsSchema.put("type", mapOpenApiTypeToJsonSchemaType(param.items().type()));
                    if (param.items().format() != null) {
                        itemsSchema.put("format", param.items().format());
                    }
                    paramSchema.set("items", itemsSchema);
                }

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
        } catch (Exception e) {
            return "{}";
        }
    }

    private String mapOpenApiTypeToJsonSchemaType(String openApiType) {
        if (openApiType == null) return "string";

        return switch (openApiType.toLowerCase()) {
            case "integer", "int32", "int64" -> "integer";
            case "number", "float", "double" -> "number";
            case "boolean" -> "boolean";
            case "array" -> "array";
            case "object" -> "object";
            default -> "string";
        };
    }

    private Function<Object, String> generateFunction(OpenApiEndpoint endpoint) {
        return (Object input) -> {
            try {
                String jsonInput = objectMapper.writeValueAsString(input);
                ToolExecutionResult result = executeEndpoint(endpoint, jsonInput);
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                if (toolLoggingEnabled) {
                    log.error("Error executing endpoint function for: {} {}", endpoint.method(), endpoint.path(), e);
                }
                try {
                    return objectMapper.writeValueAsString(new ToolExecutionResult(500, "Error executing endpoint: " + e.getMessage()));
                } catch (JsonProcessingException ex) {
                    return "{\"httpStatusCode\":500,\"body\":\"Error executing endpoint: " + e.getMessage() + "\"}";
                }
            }
        };
    }

    private ToolExecutionResult executeEndpoint(OpenApiEndpoint endpoint, String input) {
        try {
            JsonNode inputJson = objectMapper.readTree(input);
            WebClient.Builder webClientBuilderWithLogging = webClientBuilder;

            if (toolLoggingEnabled) {
                webClientBuilderWithLogging = webClientBuilder.clone().filter((request, next) -> {
                    log.info("Request: {} {}", request.method(), request.url());
                    log.info("Request Headers: {}", request.headers());
                    return next.exchange(request);
                });
            }

            WebClient webClient = webClientBuilderWithLogging.build();
            String url = buildUrl(endpoint, inputJson);

            Object requestBody = null;
            if (inputJson.has("requestBody")) {
                requestBody = inputJson.get("requestBody");
                if (toolLoggingEnabled) {
                    log.info("Request Body: {}", objectMapper.writeValueAsString(requestBody));
                }
            }

            WebClient.RequestHeadersSpec<?> requestSpec = switch (endpoint.method().toLowerCase()) {
                case "get" -> webClient.get().uri(url);
                case "post" -> {
                    WebClient.RequestBodySpec bodySpec = webClient.post().uri(url);
                    yield (requestBody != null) ? bodySpec.bodyValue(requestBody) : bodySpec;
                }
                case "put" -> {
                    WebClient.RequestBodySpec bodySpec = webClient.put().uri(url);
                    yield (requestBody != null) ? bodySpec.bodyValue(requestBody) : bodySpec;
                }
                case "delete" -> webClient.delete().uri(url);
                case "patch" -> {
                    WebClient.RequestBodySpec bodySpec = webClient.patch().uri(url);
                    yield (requestBody != null) ? bodySpec.bodyValue(requestBody) : bodySpec;
                }
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + endpoint.method());
            };

            WebClient.RequestHeadersSpec<?> finalRequest = addHeaders(requestSpec, endpoint, inputJson);

            ClientResponse response = finalRequest.exchange().block();

            String responseBody = response.bodyToMono(String.class).block();

            if (toolLoggingEnabled) {
                log.info("Response Status: {}", response.statusCode());
                log.info("Response Headers: {}", response.headers().asHttpHeaders());
                log.info("Response Body: {}", responseBody);
            }

            return new ToolExecutionResult(response.statusCode().value(), responseBody);

        } catch (Exception e) {
            if (toolLoggingEnabled) {
                log.error("Error executing endpoint: {} {}", endpoint.method(), endpoint.path(), e);
            }
            return new ToolExecutionResult(500, "Error: " + e.getMessage());
        }
    }

    private String buildUrl(OpenApiEndpoint endpoint, JsonNode inputJson) {
        String url = endpoint.baseUrl() + endpoint.path();

        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                if (inputJson.has(param.name())) {
                    String value = inputJson.get(param.name()).asText();
                    if ("path".equalsIgnoreCase(param.in())) {
                        url = url.replace("{" + param.name() + "}", value);
                    }
                }
            }
        }
        return url;
    }

    private WebClient.RequestHeadersSpec<?> addHeaders(WebClient.RequestHeadersSpec<?> request, OpenApiEndpoint endpoint, JsonNode inputJson) {
        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                if ("header".equalsIgnoreCase(param.in()) && inputJson.has(param.name())) {
                    request.header(param.name(), inputJson.get(param.name()).asText());
                }
            }
        }
        return request;
    }

    public List<FunctionCallback> convertToFunctionCallbacks(List<DynamicTool> tools) {
        List<FunctionCallback> functionCallbacks = new ArrayList<>();
        for (DynamicTool tool : tools) {
            FunctionCallbackWrapper<Object, String> f = FunctionCallbackWrapper.builder(tool.getFunction())
                    .withName(tool.getName())
                    .withDescription(tool.getDescription())
                    .withInputTypeSchema(tool.getJsonSchema())
                    .build();
            functionCallbacks.add(f);
        }
        return functionCallbacks;
    }
} 
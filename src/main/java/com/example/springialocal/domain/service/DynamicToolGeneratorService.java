package com.example.springialocal.domain.service;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import com.example.springialocal.domain.tool.model.DynamicTool;
import com.example.springialocal.domain.tool.model.OpenApiEndpoint;
import com.example.springialocal.domain.tool.model.OpenApiParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

@Service
public class DynamicToolGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolGeneratorService.class);

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public DynamicToolGeneratorService(ObjectMapper objectMapper, WebClient.Builder webClientBuilder) {
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
    }

    public List<DynamicTool> generateToolsFromEndpoints(List<OpenApiEndpoint> endpoints) {
        System.out.println("---> generateToolsFromEndpoints");
        
        List<DynamicTool> tools = new ArrayList<>();
        
        for (OpenApiEndpoint endpoint : endpoints) {
            try {
                DynamicTool tool = generateToolFromEndpoint(endpoint);
                tools.add(tool);
                log.debug("Generated tool: {} for endpoint: {} {}", tool.getName(), endpoint.method(), endpoint.path());
            } catch (Exception e) {
                log.error("Error generating tool for endpoint: {} {}", endpoint.method(), endpoint.path(), e);
            }
        }
        
        log.info("Generated {} tools from {} endpoints", tools.size(), endpoints.size());
        return tools;
    }

    private DynamicTool generateToolFromEndpoint(OpenApiEndpoint endpoint) {
        String toolName = generateToolName(endpoint);
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

    private String generateToolName(OpenApiEndpoint endpoint) {
        if (endpoint.operationId() != null && !endpoint.operationId().isEmpty()) {
            return endpoint.operationId();
        }
        
        // Generate name from method and path
        String path = endpoint.path().replaceAll("[^a-zA-Z0-9]", "_");
        return endpoint.method().toLowerCase() + "_" + path;
    }

    private String generateToolDescription(OpenApiEndpoint endpoint) {
        StringBuilder description = new StringBuilder();
        
        if (endpoint.summary() != null && !endpoint.summary().isEmpty()) {
            description.append(endpoint.summary());
        } else if (endpoint.description() != null && !endpoint.description().isEmpty()) {
            description.append(endpoint.description());
        } else {
            description.append("Executes ").append(endpoint.method().toUpperCase())
                      .append(" request to ").append(endpoint.path());
        }
        
        if (endpoint.parameters() != null && !endpoint.parameters().isEmpty()) {
            description.append(" Parameters: ");
            List<String> paramNames = endpoint.parameters().stream()
                    .map(p -> p.name() + (p.required() ? "*" : ""))
                    .toList();
            description.append(String.join(", ", paramNames));
        }
        
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
        
        // Add request body if present
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
            log.error("Error generating JSON schema for endpoint: {}", endpoint.operationId(), e);
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
                return executeEndpoint(endpoint, jsonInput);
            } catch (Exception e) {
                log.error("Error executing endpoint: {} {}", endpoint.method(), endpoint.path(), e);
                return "Error executing endpoint: " + e.getMessage();
            }
        };
    }

    private String executeEndpoint(OpenApiEndpoint endpoint, String input) {
        try {
            JsonNode inputJson = objectMapper.readTree(input);
            
            WebClient webClient = webClientBuilder.build();
            String url = buildUrl(endpoint, inputJson);
            
            WebClient.RequestHeadersSpec<?> request = switch (endpoint.method().toLowerCase()) {
                case "get" -> webClient.get().uri(url);
                case "post" -> {
                    WebClient.RequestBodySpec bodySpec = webClient.post().uri(url);
                    if (inputJson.has("requestBody")) {
                        yield bodySpec.bodyValue(inputJson.get("requestBody"));
                    } else {
                        yield bodySpec;
                    }
                }
                case "put" -> {
                    WebClient.RequestBodySpec bodySpec = webClient.put().uri(url);
                    if (inputJson.has("requestBody")) {
                        yield bodySpec.bodyValue(inputJson.get("requestBody"));
                    } else {
                        yield bodySpec;
                    }
                }
                case "delete" -> webClient.delete().uri(url);
                case "patch" -> {
                    WebClient.RequestBodySpec bodySpec = webClient.patch().uri(url);
                    if (inputJson.has("requestBody")) {
                        yield bodySpec.bodyValue(inputJson.get("requestBody"));
                    } else {
                        yield bodySpec;
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + endpoint.method());
            };
            
            // Add headers if needed
            request = addHeaders(request, inputJson);
            
            Mono<String> response = request.retrieve()
                    .bodyToMono(String.class)
                    .onErrorReturn("Error executing request: " + endpoint.path());
            
            return response.block();
            
        } catch (Exception e) {
            log.error("Error executing endpoint: {} {}", endpoint.path(), e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private String buildUrl(OpenApiEndpoint endpoint, JsonNode inputJson) {
        String baseUrl = endpoint.baseUrl();
        String path = endpoint.path();
        
        // Replace path parameters
        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                if ("path".equals(param.in()) && inputJson.has(param.name())) {
                    String value = inputJson.get(param.name()).asText();
                    path = path.replace("{" + param.name() + "}", value);
                }
            }
        }
        
        // Add query parameters
        List<String> queryParams = new ArrayList<>();
        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                if ("query".equals(param.in()) && inputJson.has(param.name())) {
                    String value = inputJson.get(param.name()).asText();
                    queryParams.add(param.name() + "=" + value);
                }
            }
        }
        
        String fullUrl = baseUrl + path;
        if (!queryParams.isEmpty()) {
            fullUrl += "?" + String.join("&", queryParams);
        }
        
        return fullUrl;
    }

    private WebClient.RequestHeadersSpec<?> addHeaders(WebClient.RequestHeadersSpec<?> request, JsonNode inputJson) {
        // Add common headers
        request = request.header("Content-Type", "application/json")
                        .header("Accept", "application/json");
        
        // Add custom headers from parameters
        // This could be extended to handle header parameters from the OpenAPI spec
        
        return request;
    }

    public List<FunctionCallback> convertToFunctionCallbacks(List<DynamicTool> tools) {
        System.out.println("---> convertToFunctionCallbacks");
        
        List<FunctionCallback> callbacks = new ArrayList<>();
        
        for (DynamicTool tool : tools) {
            try {
                FunctionCallback callback = FunctionCallbackWrapper.builder(tool.getFunction())
                        .withName(tool.getName())
                        .withDescription(tool.getDescription())
                        .withInputTypeSchema(tool.getJsonSchema())
                        .build();
                
                callbacks.add(callback);
                log.debug("Created function callback for tool: {}", tool.getName());
            } catch (Exception e) {
                log.error("Error creating function callback for tool: {}", tool.getName(), e);
            }
        }
        
        log.info("Created {} function callbacks from {} tools", callbacks.size(), tools.size());
        return callbacks;
    }
}


package com.example.swaggeragent.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.swaggeragent.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class OpenApiParserService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${openapi.specs.directory:openapi-specs}")
    private String openApiDirectory;

    public List<OpenApiEndpoint> parseAllOpenApiFiles() {
        List<OpenApiEndpoint> allEndpoints = new ArrayList<>();

        try {
            Path directory = Paths.get(openApiDirectory);
            if (!Files.exists(directory)) {
                log.warn("OpenAPI directory does not exist: {}", openApiDirectory);
                return allEndpoints;
            }

            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json") || path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                        .forEach(file -> {
                            try {
                                List<OpenApiEndpoint> endpoints = parseOpenApiFile(file.toFile());
                                allEndpoints.addAll(endpoints);
                                log.info("Parsed {} endpoints from file: {}", endpoints.size(), file.getFileName());
                            } catch (Exception e) {
                                log.error("Error parsing OpenAPI file: {}", file.getFileName(), e);
                            }
                        });
            }
        } catch (IOException e) {
            log.error("Error reading OpenAPI directory: {}", openApiDirectory, e);
        }

        log.info("Total endpoints parsed: {}", allEndpoints.size());
        return allEndpoints;
    }

    public List<OpenApiEndpoint> parseOpenApiFile(File file) {
        List<OpenApiEndpoint> endpoints = new ArrayList<>();

        try {
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            OpenAPI openAPI = parser.read(file.getAbsolutePath());

            if (openAPI == null) {
                log.error("Failed to parse OpenAPI file: {}", file.getName());
                return endpoints;
            }

            String baseUrl = extractBaseUrl(openAPI);

            if (openAPI.getPaths() != null) {
                openAPI.getPaths().forEach((path, pathItem) -> {
                    endpoints.addAll(extractEndpointsFromPath(path, pathItem, baseUrl));
                });
            }

        } catch (Exception e) {
            log.error("Error parsing OpenAPI file: {}", file.getName(), e);
        }

        return endpoints;
    }

    private String extractBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            Server server = openAPI.getServers().get(0);
            return server.getUrl();
        }
        return "http://localhost:8080"; // default fallback
    }

    private List<OpenApiEndpoint> extractEndpointsFromPath(String path, PathItem pathItem, String baseUrl) {
        List<OpenApiEndpoint> endpoints = new ArrayList<>();

        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();

        operations.forEach((httpMethod, operation) -> {
            try {
                OpenApiEndpoint endpoint = buildEndpoint(path, httpMethod.name(), operation, baseUrl);
                endpoints.add(endpoint);
            } catch (Exception e) {
                log.error("Error building endpoint for {} {}", httpMethod, path, e);
            }
        });

        return endpoints;
    }

    private OpenApiEndpoint buildEndpoint(String path, String method, Operation operation, String baseUrl) {
        return new OpenApiEndpoint(
                operation.getOperationId() != null ? operation.getOperationId() : generateOperationId(method, path),
                method.toLowerCase(),
                path,
                operation.getSummary(),
                operation.getDescription(),
                baseUrl,
                extractParameters(operation),
                extractRequestBody(operation),
                extractResponses(operation),
                operation.getTags()
        );
    }

    private String generateOperationId(String method, String path) {
        return method.toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "");
    }

    private List<OpenApiParameter> extractParameters(Operation operation) {
        List<OpenApiParameter> parameters = new ArrayList<>();

        if (operation.getParameters() != null) {
            operation.getParameters().forEach(parameter -> {
                parameters.add(buildParameter(parameter));
            });
        }

        return parameters;
    }

    private OpenApiParameter buildParameter(Parameter parameter) {
        Schema schema = parameter.getSchema();
        OpenApiParameterItems items = null;
        if (schema != null && "array".equals(schema.getType()) && schema.getItems() != null) {
            Schema<?> itemsSchema = schema.getItems();
            items = new OpenApiParameterItems(itemsSchema.getType(), itemsSchema.getFormat());
        }

        return new OpenApiParameter(
                parameter.getName(),
                parameter.getIn(),
                parameter.getDescription(),
                parameter.getRequired() != null ? parameter.getRequired() : false,
                schema != null ? schema.getType() : "string",
                schema != null ? schema.getFormat() : null,
                schema != null ? schema.getDefault() : null,
                schema != null && schema.getEnum() != null ?
                        schema.getEnum().stream().map(Object::toString).toList() : null,
                items);
    }

    private OpenApiRequestBody extractRequestBody(Operation operation) {
        if (operation.getRequestBody() == null) {
            return null;
        }

        Map<String, OpenApiMediaType> content = new HashMap<>();

        if (operation.getRequestBody().getContent() != null) {
            operation.getRequestBody().getContent().forEach((mediaType, mediaTypeObject) -> {
                content.put(mediaType, buildMediaType(mediaTypeObject));
            });
        }

        return new OpenApiRequestBody(
                operation.getRequestBody().getDescription(),
                operation.getRequestBody().getRequired() != null ? operation.getRequestBody().getRequired() : false,
                content);
    }

    private Map<String, OpenApiResponse> extractResponses(Operation operation) {
        Map<String, OpenApiResponse> responses = new HashMap<>();

        if (operation.getResponses() != null) {
            operation.getResponses().forEach((statusCode, response) -> {
                Map<String, OpenApiMediaType> content = new HashMap<>();

                if (response.getContent() != null) {
                    response.getContent().forEach((mediaType, mediaTypeObject) -> {
                        content.put(mediaType, buildMediaType(mediaTypeObject));
                    });
                }

                responses.put(statusCode, new OpenApiResponse(response.getDescription(), content));
            });
        }

        return responses;
    }

    private OpenApiMediaType buildMediaType(MediaType mediaType) {
        return new OpenApiMediaType(mediaType.getSchema(), mediaType.getExample());
    }
} 
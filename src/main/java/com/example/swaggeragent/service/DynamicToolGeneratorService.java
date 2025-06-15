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

    /**
     * Gera ferramentas a partir de uma lista de endpoints OpenAPI.
     * Inclui informações do projeto/arquivo no nome da ferramenta para evitar conflitos.
     * 
     * @param endpoints Lista de endpoints OpenAPI
     * @return Lista de ferramentas dinâmicas geradas
     */
    public List<DynamicTool> generateToolsFromEndpoints(List<OpenApiEndpoint> endpoints) {
        List<DynamicTool> tools = new ArrayList<>();
        List<String> usedNames = new ArrayList<>();

        for (OpenApiEndpoint endpoint : endpoints) {
            try {
                String toolName = generateToolName(endpoint, usedNames);
                usedNames.add(toolName);

                DynamicTool tool = generateToolFromEndpoint(endpoint, toolName);
                tools.add(tool);
                
                if (toolLoggingEnabled) {
                    log.debug("Ferramenta criada: {} para endpoint {} {}", toolName, endpoint.method(), endpoint.path());
                }
            } catch (Exception e) {
                if (toolLoggingEnabled) {
                    log.error("Erro ao gerar ferramenta para endpoint: {} {}", endpoint.method(), endpoint.path(), e);
                }
            }
        }
        
        log.info("Total de {} ferramentas geradas a partir dos endpoints", tools.size());
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

    /**
     * Gera o nome da ferramenta incluindo projeto, controller e operação.
     * Formato: {projeto}_{controller}_{operacao}
     * Evita conflitos entre endpoints iguais de diferentes arquivos swagger.
     * 
     * @param endpoint Endpoint OpenAPI
     * @param usedNames Lista de nomes já utilizados
     * @return Nome único da ferramenta
     */
    private String generateToolName(OpenApiEndpoint endpoint, List<String> usedNames) {
        // Extrai o nome do projeto da baseUrl ou usa um padrão
        String projectName = extractProjectName(endpoint.baseUrl());
        
        // Extrai o nome do controller das tags
        String controllerName = "general";
        if (endpoint.tags() != null && !endpoint.tags().isEmpty()) {
            controllerName = endpoint.tags().get(0)
                    .replace("-controller", "")
                    .replace("Controller", "")
                    .toLowerCase();
        }

        // Gera o nome da operação
        String operationName = endpoint.operationId() != null && !endpoint.operationId().isEmpty()
                ? endpoint.operationId()
                : endpoint.method().toLowerCase() + "_" + endpoint.path().replaceAll("[^a-zA-Z0-9]", "_");

        // Combina projeto, controller e operação
        String baseName = String.format("%s_%s_%s", projectName, controllerName, operationName)
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .toLowerCase();

        // Garante unicidade
        String toolName = baseName;
        int counter = 1;
        while (usedNames.contains(toolName)) {
            toolName = baseName + "_" + counter++;
        }

        // Garante que o nome da ferramenta não exceda 64 caracteres
        if (toolName.length() > 64) {
            toolName = toolName.substring(0, 64);
            // Se o nome truncado já existir, tenta adicionar um sufixo numérico
            while (usedNames.contains(toolName)) {
                // Remove os últimos caracteres para dar espaço ao sufixo
                String baseTruncatedName = toolName.substring(0, toolName.length() - String.valueOf(counter).length() -1);
                toolName = baseTruncatedName + "_" + counter++;
                // Garante que o novo nome também não exceda o limite
                if (toolName.length() > 64) {
                    toolName = toolName.substring(0, 64);
                }
            }
        }
        
        return toolName;
    }

    /**
     * Extrai o nome do projeto da URL base.
     * Tenta identificar padrões como localhost:3000, localhost:3001, etc.
     * 
     * @param baseUrl URL base do servidor
     * @return Nome do projeto extraído
     */
    private String extractProjectName(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "api";
        }
        
        try {
            // Extrai a porta da URL para diferenciar projetos
            if (baseUrl.contains(":")) {
                String[] parts = baseUrl.split(":");
                if (parts.length >= 3) {
                    String port = parts[2].replaceAll("[^0-9]", "");
                    switch (port) {
                        case "3000" -> { return "cards"; }
                        case "3001" -> { return "invoices"; }
                        case "3002" -> { return "proposals"; }
                        default -> { return "api_" + port; }
                    }
                }
            }
            
            // Se não conseguir extrair da porta, usa um nome genérico
            return "api";
        } catch (Exception e) {
            return "api";
        }
    }

    /**
     * Gera a descrição da ferramenta incluindo informações detalhadas do endpoint.
     * 
     * @param endpoint Endpoint OpenAPI
     * @return Descrição detalhada da ferramenta
     */
    private String generateToolDescription(OpenApiEndpoint endpoint) {
        StringBuilder description = new StringBuilder();

        // Extrai informações do projeto para contexto
        String projectName = extractProjectName(endpoint.baseUrl());
        String controllerName = endpoint.tags() != null && !endpoint.tags().isEmpty() 
                ? endpoint.tags().get(0) : "general";

        // Adiciona contexto do projeto e controller
        description.append(String.format("[%s API - %s] ", projectName.toUpperCase(), controllerName));

        if (endpoint.summary() != null && !endpoint.summary().isEmpty()) {
            description.append(endpoint.summary());
        } else if (endpoint.description() != null && !endpoint.description().isEmpty()) {
            description.append(endpoint.description());
        } else {
            description.append("Executa uma requisição ").append(endpoint.method().toUpperCase())
                    .append(" para o caminho '").append(endpoint.path()).append("'.");
        }

        if (!description.toString().endsWith(".")) {
            description.append(".");
        }

        // Adiciona informações sobre parâmetros
        if (endpoint.parameters() != null && !endpoint.parameters().isEmpty()) {
            description.append(" Parâmetros necessários: ");
            List<String> paramDetails = endpoint.parameters().stream()
                    .map(p -> String.format("%s (%s, %s)", p.name(), p.type(), p.required() ? "obrigatório" : "opcional"))
                    .toList();
            description.append(String.join("; ", paramDetails)).append(".");
        }

        // Adiciona informações sobre corpo da requisição
        if (endpoint.requestBody() != null && endpoint.requestBody().required()) {
            description.append(" Requer corpo da requisição.");
        }

        // Adiciona informações sobre respostas de sucesso
        endpoint.responses().forEach((statusCode, response) -> {
            if (statusCode.startsWith("2")) { // Respostas de sucesso
                description.append(" Em caso de sucesso (HTTP ").append(statusCode).append("), retorna ");
                if (response.content() != null && !response.content().isEmpty()) {
                    String mediaType = response.content().keySet().iterator().next();
                    String schemaRef = response.content().get(mediaType).schema() != null 
                            ? response.content().get(mediaType).schema().get$ref() : null;
                    if (schemaRef != null) {
                        description.append("um objeto do tipo '").append(schemaRef.substring(schemaRef.lastIndexOf('/') + 1)).append("'.");
                    } else {
                        description.append("uma resposta do tipo '").append(mediaType).append("'.");
                    }
                } else {
                    description.append("sem conteúdo.");
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
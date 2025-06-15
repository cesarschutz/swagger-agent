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

/**
 * Serviço responsável por fazer o parsing de arquivos OpenAPI/Swagger.
 * Este serviço varre recursivamente o diretório de especificações OpenAPI
 * e converte os endpoints encontrados em objetos OpenApiEndpoint.
 */
@Service
public class OpenApiParserService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiParserService.class);

    @Value("${openapi.specs.directory:openapi-specs}")
    private String openApiDirectory;

    /**
     * Faz o parsing de todos os arquivos OpenAPI encontrados no diretório configurado.
     * Varre recursivamente todas as subpastas procurando por arquivos .json, .yaml e .yml.
     * 
     * @return Lista de todos os endpoints encontrados em todos os arquivos
     */
    public List<OpenApiEndpoint> parseAllOpenApiFiles() {
        List<OpenApiEndpoint> allEndpoints = new ArrayList<>();

        try {
            Path directory = Paths.get(openApiDirectory);
            if (!Files.exists(directory)) {
                log.warn("Diretório de especificações OpenAPI não existe: {}", openApiDirectory);
                return allEndpoints;
            }

            log.info("Iniciando varredura recursiva do diretório: {}", openApiDirectory);

            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.toString().toLowerCase();
                            return fileName.endsWith(".json") || fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                        })
                        .forEach(file -> {
                            try {
                                log.info("Processando arquivo: {}", file.toString());
                                List<OpenApiEndpoint> endpoints = parseOpenApiFile(file.toFile());
                                allEndpoints.addAll(endpoints);
                                log.info("Extraídos {} endpoints do arquivo: {}", endpoints.size(), file.getFileName());
                            } catch (Exception e) {
                                log.error("Erro ao processar arquivo OpenAPI: {}", file.getFileName(), e);
                            }
                        });
            }
        } catch (IOException e) {
            log.error("Erro ao ler diretório de especificações OpenAPI: {}", openApiDirectory, e);
        }

        log.info("Total de endpoints extraídos de todos os arquivos: {}", allEndpoints.size());
        return allEndpoints;
    }

    /**
     * Faz o parsing de um arquivo OpenAPI específico.
     * 
     * @param file Arquivo OpenAPI a ser processado
     * @return Lista de endpoints encontrados no arquivo
     */
    public List<OpenApiEndpoint> parseOpenApiFile(File file) {
        List<OpenApiEndpoint> endpoints = new ArrayList<>();

        try {
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            OpenAPI openAPI = parser.read(file.getAbsolutePath());

            if (openAPI == null) {
                log.error("Falha ao fazer parsing do arquivo OpenAPI: {}", file.getName());
                return endpoints;
            }

            String baseUrl = extractBaseUrl(openAPI);
            log.debug("URL base extraída: {} para arquivo: {}", baseUrl, file.getName());

            if (openAPI.getPaths() != null) {
                openAPI.getPaths().forEach((path, pathItem) -> {
                    endpoints.addAll(extractEndpointsFromPath(path, pathItem, baseUrl));
                });
            }

        } catch (Exception e) {
            log.error("Erro ao processar arquivo OpenAPI: {}", file.getName(), e);
        }

        return endpoints;
    }

    /**
     * Extrai a URL base do arquivo OpenAPI.
     * Utiliza o primeiro servidor definido na especificação.
     * 
     * @param openAPI Objeto OpenAPI parseado
     * @return URL base do servidor
     */
    private String extractBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            Server server = openAPI.getServers().get(0);
            return server.getUrl();
        }
        return "http://localhost:8080"; // URL padrão de fallback
    }

    /**
     * Extrai todos os endpoints de um path específico do OpenAPI.
     * 
     * @param path Caminho do endpoint (ex: /cards/{uuid})
     * @param pathItem Objeto PathItem contendo as operações
     * @param baseUrl URL base do servidor
     * @return Lista de endpoints extraídos
     */
    private List<OpenApiEndpoint> extractEndpointsFromPath(String path, PathItem pathItem, String baseUrl) {
        List<OpenApiEndpoint> endpoints = new ArrayList<>();

        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();

        operations.forEach((httpMethod, operation) -> {
            try {
                OpenApiEndpoint endpoint = buildEndpoint(path, httpMethod.name(), operation, baseUrl);
                endpoints.add(endpoint);
                log.debug("Endpoint criado: {} {}", httpMethod, path);
            } catch (Exception e) {
                log.error("Erro ao construir endpoint para {} {}", httpMethod, path, e);
            }
        });

        return endpoints;
    }

    /**
     * Constrói um objeto OpenApiEndpoint a partir dos dados extraídos.
     * 
     * @param path Caminho do endpoint
     * @param method Método HTTP
     * @param operation Operação OpenAPI
     * @param baseUrl URL base do servidor
     * @return Objeto OpenApiEndpoint construído
     */
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

    /**
     * Gera um ID de operação quando não está definido no OpenAPI.
     * 
     * @param method Método HTTP
     * @param path Caminho do endpoint
     * @return ID de operação gerado
     */
    private String generateOperationId(String method, String path) {
        return method.toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Extrai os parâmetros de uma operação OpenAPI.
     * 
     * @param operation Operação OpenAPI
     * @return Lista de parâmetros extraídos
     */
    private List<OpenApiParameter> extractParameters(Operation operation) {
        List<OpenApiParameter> parameters = new ArrayList<>();

        if (operation.getParameters() != null) {
            operation.getParameters().forEach(parameter -> {
                parameters.add(buildParameter(parameter));
            });
        }

        return parameters;
    }

    /**
     * Constrói um objeto OpenApiParameter a partir de um Parameter do OpenAPI.
     * 
     * @param parameter Parâmetro do OpenAPI
     * @return Objeto OpenApiParameter construído
     */
    private OpenApiParameter buildParameter(Parameter parameter) {
        Schema schema = parameter.getSchema();
        OpenApiParameterItems items = null;
        
        // Processa items para parâmetros do tipo array
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

    /**
     * Extrai o corpo da requisição de uma operação OpenAPI.
     * 
     * @param operation Operação OpenAPI
     * @return Objeto OpenApiRequestBody ou null se não houver corpo
     */
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

    /**
     * Extrai as respostas de uma operação OpenAPI.
     * 
     * @param operation Operação OpenAPI
     * @return Mapa de respostas por código de status
     */
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

    /**
     * Constrói um objeto OpenApiMediaType a partir de um MediaType do OpenAPI.
     * 
     * @param mediaType MediaType do OpenAPI
     * @return Objeto OpenApiMediaType construído
     */
    private OpenApiMediaType buildMediaType(MediaType mediaType) {
        return new OpenApiMediaType(mediaType.getSchema(), mediaType.getExample());
    }
} 
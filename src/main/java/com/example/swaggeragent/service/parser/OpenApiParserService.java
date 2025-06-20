package com.example.swaggeragent.service.parser;

import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiMediaType;
import com.example.swaggeragent.model.OpenApiParameter;
import com.example.swaggeragent.model.OpenApiParameterItems;
import com.example.swaggeragent.model.OpenApiRequestBody;
import com.example.swaggeragent.model.OpenApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Serviço responsável por analisar (fazer o "parse") de arquivos de especificação OpenAPI (Swagger).
 * <p>
 * Este serviço varre um diretório configurado em busca de arquivos de API (`.json`, `.yaml`, `.yml`),
 * os analisa, extrai as definições de todos os endpoints e os converte em uma lista de
 * objetos {@link OpenApiEndpoint}. Ele também gerencia um cache das especificações OpenAPI
 * analisadas para otimizar o acesso e a resolução de referências internas (`$ref`).
 */
@Service
public class OpenApiParserService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiParserService.class);
    /**
     * URL de fallback a ser usada caso nenhuma URL de servidor seja encontrada na especificação OpenAPI.
     */
    private static final String DEFAULT_FALLBACK_URL = "http://localhost:8080";
    /**
     * Cache para armazenar as instâncias de {@link OpenAPI} já analisadas, usando o nome do projeto como chave.
     * Evita a re-análise de arquivos e facilita a resolução de referências.
     */
    private final Map<String, OpenAPI> openApiCache = new HashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * Construtor que inicializa o serviço com um {@link ObjectMapper} configurado.
     *
     * @param objectMapper o mapper JSON a ser utilizado para serializações internas.
     */
    public OpenApiParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Exclude null fields from serialization for cleaner output
        // this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Value("${openapi.specs.directory:openapi-specs}")
    private String openApiDirectory;

    /**
     * Analisa todos os arquivos de especificação OpenAPI encontrados no diretório configurado.
     * <p>
     * O método varre recursivamente o diretório, identifica arquivos suportados
     * e os processa para extrair a lista completa de endpoints.
     *
     * @return uma lista de todos os {@link OpenApiEndpoint} encontrados em todos os arquivos.
     */
    public List<OpenApiEndpoint> parseAllOpenApiFiles() {
        List<OpenApiEndpoint> allEndpoints = new ArrayList<>();
        Path directory = Paths.get(openApiDirectory);

        if (!Files.exists(directory)) {
            log.warn("Diretório de especificações OpenAPI não encontrado: {}", directory.toAbsolutePath());
            return allEndpoints;
        }

        log.info("Iniciando varredura por arquivos OpenAPI no diretório: {}", directory.toAbsolutePath());
        try (Stream<Path> files = Files.walk(directory)) {
            files.filter(this::isSupportedFile)
                 .forEach(file -> processApiFile(file, allEndpoints));
        } catch (IOException e) {
            log.error("Erro ao percorrer o diretório de especificações OpenAPI: {}", directory.toAbsolutePath(), e);
        }

        log.info("Análise concluída. Total de {} endpoints extraídos de todos os arquivos.", allEndpoints.size());
        return allEndpoints;
    }
    
    /**
     * Verifica se um caminho de arquivo é um arquivo regular com uma extensão suportada (.json, .yaml, .yml).
     *
     * @param path o caminho do arquivo a ser verificado.
     * @return {@code true} se o arquivo for suportado, {@code false} caso contrário.
     */
    private boolean isSupportedFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String fileName = path.toString().toLowerCase();
        return fileName.endsWith(".json") || fileName.endsWith(".yaml") || fileName.endsWith(".yml");
    }

    /**
     * Processa um único arquivo de API, chamando o método de análise e adicionando os endpoints
     * extraídos à lista agregada.
     *
     * @param filePath     o caminho para o arquivo de API.
     * @param allEndpoints a lista onde os endpoints extraídos serão adicionados.
     */
    private void processApiFile(Path filePath, List<OpenApiEndpoint> allEndpoints) {
        try {
            log.info("Analisando arquivo de API: {}", filePath.toString());
            List<OpenApiEndpoint> endpoints = parseOpenApiFile(filePath.toFile());
            allEndpoints.addAll(endpoints);
            log.info("Extraídos {} endpoints do arquivo: {}", endpoints.size(), filePath.getFileName());
        } catch (Exception e) {
            log.error("Falha ao processar o arquivo OpenAPI: {}", filePath.getFileName(), e);
        }
    }

    /**
     * Analisa um único arquivo de especificação OpenAPI.
     * <p>
     * Utiliza o {@link OpenAPIV3Parser} para ler e analisar o arquivo. Armazena o resultado
     * em cache e extrai informações como URL base, nome do projeto e todos os endpoints.
     *
     * @param file o arquivo OpenAPI a ser analisado.
     * @return uma lista de {@link OpenApiEndpoint} encontrados no arquivo.
     */
    public List<OpenApiEndpoint> parseOpenApiFile(File file) {
        List<OpenApiEndpoint> endpoints = new ArrayList<>();

        try {
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            ParseOptions options = new ParseOptions();
            options.setResolve(true); // Força a resolução de referências $ref, crucial para esquemas complexos.
            OpenAPI openAPI = parser.read(file.getAbsolutePath(), null, options);

            if (openAPI == null) {
                log.error("Falha ao analisar o arquivo OpenAPI, o resultado foi nulo. O arquivo pode estar mal formatado: {}", file.getName());
                return endpoints;
            }

            String baseUrl = extractBaseUrl(openAPI);
            log.debug("URL base extraída para '{}': {}", file.getName(), baseUrl);

            // Define o nome do projeto a partir do título da API ou do nome do arquivo como fallback.
            String projectName = "default-project";
            if (openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null && !openAPI.getInfo().getTitle().isEmpty()) {
                projectName = openAPI.getInfo().getTitle().replaceAll("\\s+", "-").toLowerCase();
            } else {
                projectName = file.getName().replaceFirst("[.][^.]+$", "");
                log.warn("Título da API não encontrado no arquivo: {}. Usando o nome do arquivo como fallback para o nome do projeto: '{}'", file.getName(), projectName);
            }

            final String finalProjectName = projectName;
            openApiCache.put(finalProjectName, openAPI); // Adiciona a API parseada ao cache.

            if (openAPI.getPaths() != null) {
                openAPI.getPaths().forEach((path, pathItem) -> {
                    endpoints.addAll(extractEndpointsFromPath(path, pathItem, baseUrl, finalProjectName, openAPI));
                });
            }

        } catch (Exception e) {
            log.error("Erro inesperado ao analisar o arquivo OpenAPI: {}", file.getName(), e);
        }

        return endpoints;
    }

    /**
     * Extrai a URL base da especificação OpenAPI, usando a primeira URL de servidor definida.
     *
     * @param openAPI o objeto OpenAPI analisado.
     * @return a URL base do servidor ou uma URL de fallback padrão.
     */
    private String extractBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            Server server = openAPI.getServers().get(0);
            String url = server.getUrl();
            // Garante que não retornemos uma URL vazia ou nula.
            return url != null && !url.isBlank() ? url : DEFAULT_FALLBACK_URL;
        }
        log.warn("Nenhum servidor definido na especificação OpenAPI, usando a URL de fallback: {}", DEFAULT_FALLBACK_URL);
        return DEFAULT_FALLBACK_URL;
    }

    /**
     * Extrai todos os endpoints definidos dentro de um determinado item de caminho (path item).
     * <p>
     * Um {@link PathItem} pode conter múltiplas operações (uma para cada método HTTP: GET, POST, etc.).
     * Este método itera sobre todas as operações e as converte em objetos {@link OpenApiEndpoint}.
     *
     * @param path          o caminho do endpoint (ex: "/users/{id}").
     * @param pathItem      o objeto que contém as definições das operações para o caminho.
     * @param baseUrl       a URL base da API.
     * @param projectName   o nome do projeto ao qual o endpoint pertence.
     * @param openAPI       o objeto OpenAPI raiz para resolver referências.
     * @return uma lista de {@link OpenApiEndpoint} extraídos do item de caminho.
     */
    private List<OpenApiEndpoint> extractEndpointsFromPath(String path, PathItem pathItem, String baseUrl, String projectName, OpenAPI openAPI) {
        List<OpenApiEndpoint> endpoints = new ArrayList<>();

        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();

        operations.forEach((httpMethod, operation) -> {
            try {
                OpenApiEndpoint endpoint = buildEndpoint(path, httpMethod.name(), operation, baseUrl, projectName, openAPI);
                endpoints.add(endpoint);
                log.trace("Endpoint extraído com sucesso: {} {}", httpMethod, path);
            } catch (Exception e) {
                log.error("Erro ao construir o endpoint para o caminho: {} {}, Método: {}", path, httpMethod, e);
            }
        });

        return endpoints;
    }

    /**
     * Constrói um único objeto {@link OpenApiEndpoint} a partir dos dados de uma operação.
     *
     * @param path        o caminho do endpoint.
     * @param method      o método HTTP (ex: "GET").
     * @param operation   o objeto {@link Operation} do OpenAPI.
     * @param baseUrl     a URL base da API.
     * @param projectName o nome do projeto.
     * @param openAPI     o objeto OpenAPI raiz.
     * @return um objeto {@link OpenApiEndpoint} totalmente preenchido.
     */
    private OpenApiEndpoint buildEndpoint(String path, String method, Operation operation, String baseUrl, String projectName, OpenAPI openAPI) {
        return new OpenApiEndpoint(
                operation.getOperationId() != null ? operation.getOperationId() : generateOperationId(method, path),
                method.toLowerCase(),
                path,
                operation.getSummary(),
                operation.getDescription(),
                baseUrl,
                projectName,
                extractParameters(operation, openAPI),
                extractRequestBody(operation, openAPI),
                extractResponses(operation, openAPI),
                operation.getTags()
        );
    }

    /**
     * Gera um ID de operação (operationId) de fallback quando um não é fornecido na especificação.
     * O ID gerado é uma combinação do método e do caminho, normalizado.
     *
     * @param method o método HTTP.
     * @param path   o caminho do endpoint.
     * @return um ID de operação gerado.
     */
    private String generateOperationId(String method, String path) {
        // Remove caracteres não alfanuméricos do caminho para criar um ID mais limpo.
        return method.toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Extrai todos os parâmetros (path, query, header, etc.) de uma operação.
     *
     * @param operation a operação OpenAPI.
     * @param openAPI   o objeto OpenAPI raiz.
     * @return uma lista de {@link OpenApiParameter}.
     */
    private List<OpenApiParameter> extractParameters(Operation operation, OpenAPI openAPI) {
        List<OpenApiParameter> parameters = new ArrayList<>();

        if (operation.getParameters() != null) {
            operation.getParameters().forEach(parameter -> {
                parameters.add(buildParameter(parameter, openAPI));
            });
        }
        return parameters;
    }

    /**
     * Constrói um único {@link OpenApiParameter} a partir de um objeto {@link Parameter} do OpenAPI.
     * Resolve referências ($ref) para parâmetros se necessário.
     *
     * @param parameter o parâmetro da especificação.
     * @param openAPI   o objeto OpenAPI raiz.
     * @return um objeto {@link OpenApiParameter} preenchido.
     */
    private OpenApiParameter buildParameter(Parameter parameter, OpenAPI openAPI) {
        // Resolve a referência se o parâmetro for um $ref.
        if (parameter.get$ref() != null) {
            String ref = parameter.get$ref();
            parameter = openAPI.getComponents().getParameters().get(ref.substring(ref.lastIndexOf('/') + 1));
        }

        Schema<?> schema = resolveSchema(parameter.getSchema(), openAPI);
        String type = schema != null ? schema.getType() : "string";
        String format = schema != null ? schema.getFormat() : null;
        Object defaultValue = schema != null ? schema.getDefault() : null;
        List<String> enumValues = schema != null && schema.getEnum() != null ?
                schema.getEnum().stream().map(Object::toString).toList() : null;

        OpenApiParameterItems items = null;
        if ("array".equals(type) && schema != null && schema.getItems() != null) {
            Schema<?> itemsSchema = resolveSchema(schema.getItems(), openAPI);
            if (itemsSchema != null) {
                items = new OpenApiParameterItems(itemsSchema.getType(), itemsSchema.getFormat());
            }
        }

        return new OpenApiParameter(
                parameter.getName(),
                parameter.getIn(),
                parameter.getDescription(),
                Boolean.TRUE.equals(parameter.getRequired()),
                type,
                format,
                defaultValue,
                enumValues,
                items
        );
    }

    /**
     * Extrai a definição do corpo da requisição (request body) de uma operação.
     *
     * @param operation a operação OpenAPI.
     * @param openAPI   o objeto OpenAPI raiz.
     * @return um {@link OpenApiRequestBody} ou {@code null} se não houver corpo na requisição.
     */
    private OpenApiRequestBody extractRequestBody(Operation operation, OpenAPI openAPI) {
        if (operation.getRequestBody() == null) {
            return null;
        }
        // Resolve a referência se o requestBody for um $ref.
        io.swagger.v3.oas.models.parameters.RequestBody requestBody = operation.getRequestBody();
        if (requestBody.get$ref() != null) {
            String ref = requestBody.get$ref();
            requestBody = openAPI.getComponents().getRequestBodies().get(ref.substring(ref.lastIndexOf('/') + 1));
        }

        Map<String, OpenApiMediaType> content = new HashMap<>();
        if (requestBody.getContent() != null) {
            requestBody.getContent().forEach((mediaTypeName, mediaType) ->
                    content.put(mediaTypeName, buildMediaType(mediaType, openAPI)));
        }

        return new OpenApiRequestBody(
                requestBody.getDescription(),
                Boolean.TRUE.equals(requestBody.getRequired()),
                content
        );
    }

    /**
     * Extrai as definições de todas as possíveis respostas de uma operação.
     *
     * @param operation a operação OpenAPI.
     * @param openAPI   o objeto OpenAPI raiz.
     * @return um mapa de {@link OpenApiResponse} onde a chave é o código de status HTTP.
     */
    private Map<String, OpenApiResponse> extractResponses(Operation operation, OpenAPI openAPI) {
        Map<String, OpenApiResponse> responses = new HashMap<>();
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((code, apiResponse) -> {
                io.swagger.v3.oas.models.responses.ApiResponse resolvedResponse = apiResponse;
                // Resolve a referência se a resposta for um $ref.
                if (apiResponse.get$ref() != null) {
                    String ref = apiResponse.get$ref();
                    resolvedResponse = openAPI.getComponents().getResponses().get(ref.substring(ref.lastIndexOf('/') + 1));
                }

                if (resolvedResponse != null && resolvedResponse.getContent() != null) {
                    Map<String, OpenApiMediaType> content = new HashMap<>();
                    resolvedResponse.getContent().forEach((type, mediaType) ->
                            content.put(type, buildMediaType(mediaType, openAPI))
                    );
                    responses.put(code, new OpenApiResponse(resolvedResponse.getDescription(), content, resolvedResponse.getHeaders()));
                } else {
                    // Adiciona a resposta mesmo que não tenha conteúdo (ex: 204 No Content)
                    String description = resolvedResponse != null ? resolvedResponse.getDescription() : "No description";
                    Map<String, Header> headers = resolvedResponse != null ? resolvedResponse.getHeaders() : null;
                    responses.put(code, new OpenApiResponse(description, null, headers));
                }
            });
        }
        return responses;
    }

    /**
     * Constrói um objeto {@link OpenApiMediaType} a partir de um {@link MediaType} do OpenAPI.
     * <p>
     * Este método resolve o schema de referência e, crucialmente, extrai o campo 'example'
     * do media type, que é usado para fornecer exemplos concretos de payloads.
     *
     * @param mediaType o objeto MediaType da especificação.
     * @param openAPI   o objeto OpenAPI raiz para resolver referências.
     * @return um objeto OpenApiMediaType contendo o schema e o exemplo.
     */
    private OpenApiMediaType buildMediaType(MediaType mediaType, OpenAPI openAPI) {
        if (mediaType == null) {
            return null;
        }
        Schema<?> resolvedSchema = resolveSchema(mediaType.getSchema(), openAPI);

        Object example = mediaType.getExample();

        if (example == null && mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
            // Tenta pegar o primeiro exemplo da lista de exemplos se o campo 'example' estiver vazio.
            example = mediaType.getExamples().values().iterator().next().getValue();
        }

        return new OpenApiMediaType(resolvedSchema, example, mediaType.getExamples());
    }

    /**
     * Resolve um esquema (schema), seguindo referências $ref se necessário.
     *
     * @param schema  o esquema a ser resolvido.
     * @param openAPI o objeto OpenAPI raiz para procurar componentes referenciados.
     * @return o esquema resolvido, ou o esquema original se não for uma referência.
     */
    private Schema<?> resolveSchema(Schema<?> schema, OpenAPI openAPI) {
        if (schema != null && schema.get$ref() != null) {
            return lookupSchema(schema, openAPI);
        }
        return schema;
    }

    /**
     * Procura e retorna um esquema referenciado a partir do repositório de componentes do OpenAPI.
     *
     * @param schema  o esquema que contém a referência $ref.
     * @param openAPI o objeto OpenAPI raiz.
     * @return o esquema encontrado na seção de componentes.
     */
    public Schema<?> lookupSchema(Schema<?> schema, OpenAPI openAPI) {
        String ref = schema.get$ref();
        if (ref == null) {
            return schema;
        }
        // Extrai o nome do componente da referência (ex: #/components/schemas/Pet -> Pet)
        String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
        Schema<?> resolvedSchema = openAPI.getComponents().getSchemas().get(schemaName);

        if (resolvedSchema == null) {
            log.warn("Não foi possível resolver a referência do esquema: {}", ref);
        }
        return resolvedSchema;
    }

    /**
     * Converte um esquema OpenAPI em uma representação de string JSON.
     * <p>
     * Este método é útil para exibir o esquema em logs ou descrições de ferramentas.
     *
     * @param projectName o nome do projeto para localizar a API no cache.
     * @param schema      o esquema a ser convertido.
     * @return uma string JSON representando o esquema.
     */
    public String getSchemaAsJson(String projectName, io.swagger.v3.oas.models.media.Schema<?> schema) {
        OpenAPI openAPI = openApiCache.get(projectName);
        if (openAPI == null) {
            return "{\"error\": \"Especificação OpenAPI não encontrada para o projeto: " + projectName + "\"}";
        }
        try {
            ObjectNode schemaNode = buildJsonSchemaNode(schema, openAPI, new HashSet<>());
            return objectMapper.writeValueAsString(schemaNode);
        } catch (Exception e) {
            log.error("Erro ao converter schema para JSON para o projeto {}", projectName, e);
            return "{\"error\": \"Falha ao converter o schema para JSON\"}";
        }
    }

    /**
     * Resolve um esquema (incluindo referências $ref) e o retorna como um {@link ObjectNode} JSON.
     *
     * @param projectName o nome do projeto para encontrar a especificação OpenAPI no cache.
     * @param schema      o esquema a ser resolvido.
     * @return um {@link ObjectNode} representando o esquema resolvido, ou null em caso de erro.
     */
    public ObjectNode getResolvedSchemaAsJsonNode(String projectName, Schema<?> schema) {
        OpenAPI openAPI = openApiCache.get(projectName);
        if (openAPI == null) {
            log.warn("Nenhuma especificação OpenAPI encontrada no cache para o projeto: {}", projectName);
            return null;
        }
        return buildJsonSchemaNode(schema, openAPI, new HashSet<>());
    }

    /**
     * Constrói recursivamente um {@link ObjectNode} JSON a partir de um esquema OpenAPI.
     * <p>
     * Este método navega pela estrutura do esquema, incluindo propriedades e referências aninhadas,
     * para criar uma representação JSON completa. Evita recursão infinita rastreando
     * as referências já visitadas.
     *
     * @param schema       o esquema a ser convertido.
     * @param openAPI      o objeto OpenAPI raiz.
     * @param visitedRefs  um conjunto de referências já visitadas para evitar loops.
     * @return um {@link ObjectNode} que representa o esquema.
     */
    private ObjectNode buildJsonSchemaNode(io.swagger.v3.oas.models.media.Schema<?> schema, OpenAPI openAPI, Set<String> visitedRefs) {
        ObjectNode node = objectMapper.createObjectNode();
        if (schema == null) {
            return node;
        }

        // Se o esquema for uma referência, resolve-o primeiro.
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            // Proteção contra recursão infinita.
            if (visitedRefs.contains(ref)) {
                return node.put("description", "Referência circular para " + ref);
            }
            visitedRefs.add(ref);
            Schema<?> resolvedSchema = lookupSchema(schema, openAPI);
            // Chamada recursiva para construir o nó do esquema resolvido.
            return buildJsonSchemaNode(resolvedSchema, openAPI, visitedRefs);
        }

        if (schema.getType() != null) node.put("type", schema.getType());
        if (schema.getFormat() != null) node.put("format", schema.getFormat());
        if (schema.getDescription() != null) node.put("description", schema.getDescription());

        // Processa as propriedades de um esquema do tipo objeto.
        if ("object".equals(schema.getType()) && schema.getProperties() != null) {
            ObjectNode propertiesNode = objectMapper.createObjectNode();
            schema.getProperties().forEach((key, value) -> {
                Schema<?> propSchema = (Schema<?>) value;
                // Chamada recursiva para cada propriedade.
                propertiesNode.set(key.toString(), buildJsonSchemaNode(propSchema, openAPI, new HashSet<>(visitedRefs)));
            });
            node.set("properties", propertiesNode);
        }

        // Processa os itens de um esquema do tipo array.
        if ("array".equals(schema.getType()) && schema.getItems() != null) {
            // Chamada recursiva para o esquema dos itens do array.
            node.set("items", buildJsonSchemaNode(schema.getItems(), openAPI, new HashSet<>(visitedRefs)));
        }

        return node;
    }
} 
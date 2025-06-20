package com.example.swaggeragent.service.tool;

import com.example.swaggeragent.model.domain.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiParameter;
import com.example.swaggeragent.model.response.ToolExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.swaggeragent.service.parser.OpenApiParserService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela geração de ferramentas (tools) dinâmicas a partir de endpoints OpenAPI.
 * <p>
 * <b>Responsabilidades:</b>
 * <ul>
 *   <li>Converter uma lista de {@link OpenApiEndpoint} em uma lista de {@link DynamicTool}.</li>
 *   <li>Gerar nomes únicos e descritivos para cada ferramenta, evitando conflitos.</li>
 *   <li>Criar esquemas JSON (JSON Schema) para os parâmetros das ferramentas.</li>
 *   <li>Gerar a função de execução ({@link java.util.function.Function}) que realiza a chamada à API.</li>
 *   <li>Transformar as ferramentas dinâmicas em {@link org.springframework.ai.model.function.FunctionCallback} para uso pelo Spring AI.</li>
 * </ul>
 *
 * <b>Dica para devs juniores:</b> Cada método privado tem um papel específico na construção das ferramentas. Leia os comentários para entender como cada etapa contribui para o processo.
 */
@Service
public class DynamicToolGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolGeneratorService.class);

    private static final String PARAM_IN_PATH = "path";
    private static final String PARAM_IN_QUERY = "query";
    private static final String PARAM_IN_HEADER = "header";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_TRAFFIC_CODE = "Traffic-Code";
    private static final int MAX_TOOL_NAME_LENGTH = 64;

    private final ObjectMapper objectMapper;
    private final OpenApiParserService openApiParserService;
    private final WebClient webClient;
    private final String apiAuthorizationToken;
    private final String apiTrafficCode;

    /**
     * Construtor para injeção de dependências e configuração dos valores de segurança.
     *
     * @param objectMapper          o serializador/desserializador JSON.
     * @param openApiParserService  o serviço para analisar especificações OpenAPI.
     * @param webClient             o cliente reativo para chamadas HTTP.
     * @param apiAuthorizationToken o token de autorização Bearer para as APIs.
     * @param apiTrafficCode        o código de tráfego para as APIs.
     */
    public DynamicToolGeneratorService(
            ObjectMapper objectMapper,
            OpenApiParserService openApiParserService,
            WebClient webClient,
            @Value("${api.security.authorization.token}") String apiAuthorizationToken,
            @Value("${api.security.traffic-code}") String apiTrafficCode) {
        this.objectMapper = objectMapper;
        this.openApiParserService = openApiParserService;
        this.webClient = webClient;
        this.apiAuthorizationToken = apiAuthorizationToken;
        this.apiTrafficCode = apiTrafficCode;
    }

    /**
     * Gera uma lista de {@link DynamicTool} a partir de uma lista de {@link OpenApiEndpoint}.
     * <p>
     * Garante que cada ferramenta tenha um nome único, combinando o nome do projeto,
     * o nome do controller (tag) e o ID da operação para evitar colisões.
     *
     * @param endpoints a lista de endpoints extraídos da especificação OpenAPI.
     * @return uma lista de ferramentas dinâmicas prontas para serem usadas.
     */
    public List<DynamicTool> generateToolsFromEndpoints(List<OpenApiEndpoint> endpoints) {
        Set<String> usedNames = new HashSet<>();

        List<DynamicTool> tools = endpoints.stream()
                .map(endpoint -> {
                    try {
                        String toolName = generateUniqueToolName(endpoint, usedNames);
                        usedNames.add(toolName);
                        DynamicTool tool = generateToolFromEndpoint(endpoint, toolName);
                        log.info("✅ Ferramenta Adicionada: [Nome: {}] para o endpoint [{} {}]\n---\n{}\n---", tool.getName(), endpoint.method().toUpperCase(), endpoint.path(), tool.getDescription());
                        return tool;
                    } catch (Exception e) {
                        log.error("Falha ao gerar ferramenta para o endpoint: {} {}", endpoint.method().toUpperCase(), endpoint.path(), e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Total de {} ferramentas geradas com sucesso a partir dos endpoints.", tools.size());
        return tools;
    }

    /**
     * Converte um texto de formato camelCase ou com espaços para snake_case.
     * <p>
     * Exemplo: "getCardById" se torna "get_card_by_id".
     *
     * @param text o texto a ser convertido.
     * @return o texto em formato snake_case.
     */
    private String convertToSnakeCase(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Adiciona um underscore antes de cada letra maiúscula que segue uma minúscula ou número.
        String snake = text.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                // Substitui qualquer sequência de não alfanuméricos por um único underscore.
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .toLowerCase();
        // Remove underscores que possam ter ficado no início ou no fim.
        return snake.replaceAll("^_+|_+$", "");
    }

    /**
     * Cria uma única instância de {@link DynamicTool} a partir de um {@link OpenApiEndpoint}.
     *
     * @param endpoint o endpoint da API.
     * @param toolName o nome único já gerado para a ferramenta.
     * @return a instância de {@link DynamicTool} configurada.
     */
    private DynamicTool generateToolFromEndpoint(OpenApiEndpoint endpoint, String toolName) {
        String description = generateToolDescription(endpoint);
        String summary = endpoint.summary() != null ? endpoint.summary() : "Nenhum resumo disponível.";
        String jsonSchema = generateJsonSchema(endpoint);
        Function<Object, String> function = generateFunction(endpoint);

        return DynamicTool.builder()
                .name(toolName)
                .description(description)
                .summary(summary)
                .endpoint(endpoint)
                .function(function)
                .jsonSchema(jsonSchema)
                .build();
    }

    /**
     * Gera um nome de ferramenta único, estruturado e legível.
     * O formato é: {projeto}-{controller}-{operacao}, tudo em snake_case.
     *
     * @param endpoint  o endpoint da API.
     * @param usedNames o conjunto de nomes já em uso para evitar colisões.
     * @return um nome de ferramenta único e formatado.
     */
    private String generateUniqueToolName(OpenApiEndpoint endpoint, Set<String> usedNames) {
        String baseName = buildBaseToolName(endpoint);
        String uniqueName = ensureNameIsUnique(baseName, usedNames);
        return truncateName(uniqueName, usedNames);
    }

    /**
     * Constrói o nome base da ferramenta no formato {projeto}-{controller}-{operacao}.
     *
     * @param endpoint o endpoint da API.
     * @return o nome base da ferramenta.
     */
    private String buildBaseToolName(OpenApiEndpoint endpoint) {
        String projectName = convertToSnakeCase(endpoint.projectName() != null ? endpoint.projectName() : "default");

        String controllerName;
        if (endpoint.tags() != null && !endpoint.tags().isEmpty()) {
            // Remove sufixos comuns de nomes de controller para um nome mais limpo.
            controllerName = endpoint.tags().get(0).replace("-controller", "").replace("Controller", "");
        } else {
            controllerName = "general";
        }
        controllerName = convertToSnakeCase(controllerName);

        // Se o operationId não estiver disponível, cria um nome a partir do método e do path.
        String operationName = endpoint.operationId() != null && !endpoint.operationId().isEmpty()
                ? endpoint.operationId()
                : endpoint.method().toLowerCase() + " " + endpoint.path();
        String snakeCaseOperationName = convertToSnakeCase(operationName);

        return String.format("%s-%s-%s", projectName, controllerName, snakeCaseOperationName);
    }

    /**
     * Garante que o nome base seja único, adicionando um sufixo numérico se necessário.
     *
     * @param baseName  o nome base a ser verificado.
     * @param usedNames o conjunto de nomes já em uso.
     * @return um nome garantidamente único.
     */
    private String ensureNameIsUnique(String baseName, Set<String> usedNames) {
        String toolName = baseName;
        int counter = 1;
        while (usedNames.contains(toolName)) {
            toolName = baseName + "_" + counter++;
        }
        return toolName;
    }

    /**
     * Trunca o nome da ferramenta se exceder o comprimento máximo, garantindo ainda a unicidade.
     *
     * @param toolName  o nome a ser truncado.
     * @param usedNames o conjunto de nomes já em uso.
     * @return um nome único e com o comprimento adequado.
     */
    private String truncateName(String toolName, Set<String> usedNames) {
        if (toolName.length() <= MAX_TOOL_NAME_LENGTH) {
            return toolName;
        }

        log.warn("O nome da ferramenta '{}' excede o comprimento máximo de {} caracteres e será truncado.", toolName, MAX_TOOL_NAME_LENGTH);
        String truncatedName = toolName.substring(0, MAX_TOOL_NAME_LENGTH);
        int counter = 1;

        // Se o nome truncado já existir, adiciona um sufixo numérico.
        while (usedNames.contains(truncatedName)) {
            String baseTruncatedName = toolName.substring(0, MAX_TOOL_NAME_LENGTH - String.valueOf(counter).length() - 1);
            truncatedName = baseTruncatedName + "_" + counter++;
        }
        return truncatedName;
    }

    /**
     * Gera uma descrição rica e detalhada para a ferramenta, formatada em Markdown.
     * <p>
     * A descrição é crucial para que o modelo de linguagem entenda o propósito e o funcionamento
     * da ferramenta. Ela inclui:
     * <ul>
     *   <li>Contexto da API (nome do projeto e do controller).</li>
     *   <li>Resumo da operação.</li>
     *   <li>Lista detalhada de parâmetros (path, query, header), exceto os de segurança.</li>
     *   <li>Descrição do corpo da requisição, incluindo o esquema JSON.</li>
     *   <li>Lista de possíveis respostas com seus códigos de status e esquemas.</li>
     * </ul>
     *
     * @param endpoint o endpoint da API para o qual a descrição será gerada.
     * @return uma string contendo a descrição formatada da ferramenta.
     */
    private String generateToolDescription(OpenApiEndpoint endpoint) {
        StringBuilder description = new StringBuilder();

        description.append(String.format("API: %s. Controller: %s. ", endpoint.projectName(), getControllerName(endpoint)));
        description.append(String.format("Resumo: %s\n", endpoint.summary() != null ? endpoint.summary() : "Nenhum resumo disponível."));

        if (endpoint.parameters() != null && !endpoint.parameters().isEmpty()) {
            description.append("Parâmetros da Requisição:\n");
            endpoint.parameters().stream()
                    .filter(p -> !HEADER_AUTHORIZATION.equalsIgnoreCase(p.name()) && !HEADER_TRAFFIC_CODE.equalsIgnoreCase(p.name()))
                    .forEach(p -> description.append(String.format("- **%s** (%s, %s): %s%s\n",
                            p.name(), p.in(), p.type(), p.description(), p.required() ? " (obrigatório)" : "")));
        }

        if (endpoint.requestBody() != null && endpoint.requestBody().content() != null && !endpoint.requestBody().content().isEmpty()) {
            description.append("Corpo da Requisição (Body):\n");
            try {
                // Pega o primeiro media type (geralmente application/json)
                var firstMediaType = endpoint.requestBody().content().values().iterator().next();
                if (firstMediaType.schema() != null) {
                    String schemaJson = openApiParserService.getSchemaAsJson(endpoint.projectName(), firstMediaType.schema());
                    description.append("```json\n").append(schemaJson).append("\n```\n");
                }
            } catch (Exception e) {
                log.error("Erro ao gerar o schema JSON para o corpo da requisição da ferramenta: {}", endpoint.operationId(), e);
                description.append("Erro ao gerar o schema do corpo da requisição.\n");
            }
        }

        if (endpoint.responses() != null && !endpoint.responses().isEmpty()) {
            description.append("Respostas Possíveis:\n");
            endpoint.responses().forEach((code, response) -> {
                description.append(String.format("- **%s**: %s\n", code, response.description()));

                if (response.headers() != null && !response.headers().isEmpty()) {
                    description.append("  - Headers da Resposta:\n");
                    response.headers().forEach((name, header) ->
                            description.append(String.format("    - **%s**: %s\n", name, header.getDescription()))
                    );
                }

                if (response.content() != null && !response.content().isEmpty()) {
                    response.content().forEach((type, mediaType) -> {
                        boolean exampleFound = false;
                        // Prioridade 1: Múltiplos exemplos nomeados
                        if (mediaType.examples() != null && !mediaType.examples().isEmpty()) {
                            description.append("  - Exemplos de resposta (`").append(type).append("`):\n");
                            mediaType.examples().forEach((name, example) -> {
                                description.append("    - Exemplo '").append(name).append("':\n");
                                if (example.getDescription() != null) {
                                    description.append("      > ").append(example.getDescription()).append("\n");
                                }
                                if (example.getValue() != null) {
                                    try {
                                        description.append("      ```json\n").append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example.getValue())).append("\n      ```\n");
                                    } catch (JsonProcessingException e) {
                                        log.error("Erro ao serializar exemplo nomeado '{}' para a ferramenta: {}", name, endpoint.operationId(), e);
                                    }
                                }
                            });
                            exampleFound = true;
                        }
                        // Prioridade 2: Exemplo único
                        else if (mediaType.example() != null) {
                            description.append("  - Exemplo de resposta (`").append(type).append("`):\n");
                            try {
                                description.append("    ```json\n").append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mediaType.example())).append("\n    ```\n");
                            } catch (JsonProcessingException e) {
                                log.error("Erro ao serializar exemplo único para a ferramenta: {}", endpoint.operationId(), e);
                            }
                            exampleFound = true;
                        }

                        // Prioridade 3: Gerar exemplo a partir do Schema
                        if (!exampleFound && mediaType.schema() != null) {
                            description.append("  - Estrutura da resposta (`").append(type).append("`):\n");
                            try {
                                String schemaJson = openApiParserService.getSchemaAsJson(endpoint.projectName(), mediaType.schema());
                                description.append("    ```json\n").append(schemaJson).append("\n    ```\n");
                            } catch (Exception e) {
                                log.error("Erro ao gerar o schema JSON para a resposta da ferramenta: {}", endpoint.operationId(), e);
                            }
                        }
                    });
                }
            });
        }

        return description.toString();
    }

    /**
     * Extrai o nome do controlador (ou tag) do endpoint.
     *
     * @param endpoint o endpoint da API.
     * @return o nome da primeira tag ou "general" se nenhuma tag for definida.
     */
    private String getControllerName(OpenApiEndpoint endpoint) {
        if (endpoint.tags() != null && !endpoint.tags().isEmpty()) {
            return endpoint.tags().get(0);
        }
        return "general";
    }

    /**
     * Gera uma definição de JSON Schema para os parâmetros de entrada de uma ferramenta.
     * <p>
     * Este esquema é usado pelo modelo de linguagem para saber quais argumentos fornecer
     * e em qual formato ao chamar a ferramenta.
     * O esquema inclui tanto os parâmetros (path, query, header) quanto o corpo da requisição.
     *
     * @param endpoint o endpoint da API.
     * @return uma string contendo a definição do JSON Schema.
     */
    private String generateJsonSchema(OpenApiEndpoint endpoint) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        List<String> required = new ArrayList<>();

        // Adiciona parâmetros (path, query, header) ao esquema.
        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                String paramName = param.name();
                // Ignora parâmetros de segurança que são injetados automaticamente.
                if (HEADER_AUTHORIZATION.equalsIgnoreCase(paramName) || HEADER_TRAFFIC_CODE.equalsIgnoreCase(paramName)) {
                    continue;
                }

                ObjectNode paramSchema = objectMapper.createObjectNode();
                paramSchema.put("type", mapOpenApiTypeToJsonSchemaType(param.type()));
                if (param.description() != null) {
                    paramSchema.put("description", param.description());
                }
                if (param.enumValues() != null && !param.enumValues().isEmpty()) {
                    paramSchema.set("enum", objectMapper.valueToTree(param.enumValues()));
                }
                properties.set(paramName, paramSchema);

                if (param.required()) {
                    required.add(paramName);
                }
            }
        }

        // Adiciona o esquema do corpo da requisição, se houver.
        if (endpoint.requestBody() != null && endpoint.requestBody().content() != null) {
            endpoint.requestBody().content().forEach((mediaType, mediaTypeObject) -> {
                if (mediaTypeObject.schema() != null) {
                    // O método getResolvedSchemaAsJsonNode resolve referências ($ref) e retorna o esquema completo.
                    JsonNode requestBodySchema = openApiParserService.getResolvedSchemaAsJsonNode(endpoint.projectName(), mediaTypeObject.schema());
                    if (requestBodySchema != null && requestBodySchema.has("properties")) {
                        // Adiciona as propriedades do corpo da requisição ao esquema de parâmetros da ferramenta.
                        requestBodySchema.get("properties").fields().forEachRemaining(entry -> {
                            properties.set(entry.getKey(), entry.getValue());
                        });
                        // Adiciona os campos obrigatórios do corpo da requisição à lista de requeridos da ferramenta.
                        if (requestBodySchema.has("required")) {
                            requestBodySchema.get("required").forEach(node -> required.add(node.asText()));
                        }
                    }
                }
            });
        }


        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.set("required", objectMapper.valueToTree(required));
        }

        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            log.error("Erro ao gerar JSON Schema para o endpoint: {} {}", endpoint.method(), endpoint.path(), e);
            return "{}"; // Retorna um objeto vazio em caso de erro.
        }
    }

    /**
     * Mapeia os tipos de dados da especificação OpenAPI para os tipos de dados do JSON Schema.
     *
     * @param openApiType o tipo de dado do OpenAPI (ex: "integer", "string").
     * @return o tipo de dado correspondente no JSON Schema.
     */
    private String mapOpenApiTypeToJsonSchemaType(String openApiType) {
        if (openApiType == null) {
            return "string"; // Default para string se o tipo for nulo.
        }
        return switch (openApiType) {
            case "integer" -> "number";
            case "boolean", "string", "number" -> openApiType;
            default -> "string"; // Default para outros tipos não mapeados.
        };
    }

    /**
     * Gera a {@link java.util.function.Function} que será encapsulada pela ferramenta.
     * <p>
     * Esta função recebe um objeto (que será um JSON de entrada), o converte para {@link JsonNode},
     * e então chama {@link #executeEndpoint} para realizar a chamada HTTP.
     * O resultado é formatado como uma string JSON.
     *
     * @param endpoint o endpoint da API que a função irá chamar.
     * @return uma {@link java.util.function.Function} que executa a lógica da ferramenta.
     */
    private Function<Object, String> generateFunction(OpenApiEndpoint endpoint) {
        return input -> {
            try {
                // A entrada (input) vem como uma String JSON, que precisa ser parseada.
                String jsonInput = objectMapper.writeValueAsString(input);
                ToolExecutionResult result = executeEndpoint(endpoint, jsonInput);
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                log.error("Erro ao processar a entrada/saída JSON para a ferramenta '{}'", endpoint.operationId(), e);
                return createErrorResponse("Erro interno ao processar JSON: " + e.getMessage(), 500);
            } catch (Exception e) {
                log.error("Erro inesperado ao executar a ferramenta '{}'", endpoint.operationId(), e);
                return createErrorResponse("Erro inesperado na execução da ferramenta: " + e.getMessage(), 500);
            }
        };
    }

    /**
     * Executa a chamada HTTP para o endpoint da API usando {@link WebClient}.
     * <p>
     * Monta a requisição (URL, headers, body) com base nos parâmetros recebidos
     * e na definição do endpoint.
     *
     * @param endpoint o endpoint a ser chamado.
     * @param input    uma string JSON contendo os argumentos para a chamada.
     * @return um {@link ToolExecutionResult} com o status e o corpo da resposta.
     */
    private ToolExecutionResult executeEndpoint(OpenApiEndpoint endpoint, String input) {
        try {
            JsonNode inputJson = objectMapper.readTree(input);
            String url = buildUrl(endpoint, inputJson);

            WebClient.RequestBodySpec request = webClient
                    .method(HttpMethod.valueOf(endpoint.method().toUpperCase()))
                    .uri(url)
                    .headers(httpHeaders -> addHeaders(httpHeaders, endpoint, inputJson));

            WebClient.ResponseSpec responseSpec;
            if (isBodySupported(HttpMethod.valueOf(endpoint.method().toUpperCase()))) {
                String requestBody = getRequestBody(inputJson, endpoint.parameters());
                responseSpec = request.bodyValue(requestBody).retrieve();
            } else {
                responseSpec = request.retrieve();
            }

            // Realiza a chamada e bloqueia aguardando a resposta, tratando diferentes status.
            return responseSpec
                    .toEntity(String.class)
                    .map(responseEntity -> {
                        log.info("Execução da ferramenta '{}' concluída com status: {}", endpoint.operationId(), responseEntity.getStatusCode().value());
                        log.debug("Resposta da ferramenta '{}': {}", endpoint.operationId(), responseEntity.getBody());
                        return new ToolExecutionResult(responseEntity.getStatusCode().value(), responseEntity.getBody());
                    })
                    .block();

        } catch (Exception e) {
            log.error("Falha ao executar a chamada para o endpoint '{}' ({} {})", endpoint.operationId(), endpoint.method(), endpoint.path(), e);
            throw new RuntimeException("Falha na execução do endpoint: " + e.getMessage(), e);
        }
    }

    /**
     * Constrói a URL final para a chamada da API, substituindo os parâmetros de path.
     *
     * @param endpoint  o endpoint da API.
     * @param inputJson o JSON com os argumentos.
     * @return a URL completa com os parâmetros de path substituídos.
     */
    private String buildUrl(OpenApiEndpoint endpoint, JsonNode inputJson) {
        String path = endpoint.path();
        // Substitui placeholders no path (ex: /users/{id}) pelos valores do JSON de entrada.
        if (endpoint.parameters() != null) {
            for (OpenApiParameter param : endpoint.parameters()) {
                if (PARAM_IN_PATH.equals(param.in()) && inputJson.has(param.name())) {
                    path = path.replace("{" + param.name() + "}", inputJson.get(param.name()).asText());
                }
            }
        }
        return endpoint.baseUrl() + path;
    }

    /**
     * Adiciona os cabeçalhos (headers) à requisição WebClient.
     * <p>
     * Inclui cabeçalhos de segurança (Authorization, Traffic-Code) e quaisquer outros
     * definidos na especificação OpenAPI que estejam presentes no JSON de entrada.
     *
     * @param httpHeaders o builder de cabeçalhos do WebClient.
     * @param endpoint    o endpoint da API.
     * @param inputJson   o JSON com os argumentos.
     */
    private void addHeaders(org.springframework.http.HttpHeaders httpHeaders, OpenApiEndpoint endpoint, JsonNode inputJson) {
        // Adiciona cabeçalhos de segurança padrão.
        httpHeaders.set(HEADER_AUTHORIZATION, "Bearer " + apiAuthorizationToken);
        httpHeaders.set(HEADER_TRAFFIC_CODE, apiTrafficCode);
        httpHeaders.set("Content-Type", "application/json"); // Define o tipo de conteúdo padrão.

        // Adiciona cabeçalhos específicos do endpoint.
        if (endpoint.parameters() != null) {
            endpoint.parameters().stream()
                    .filter(p -> PARAM_IN_HEADER.equals(p.in()) && inputJson.has(p.name()))
                    .forEach(p -> httpHeaders.set(p.name(), inputJson.get(p.name()).asText()));
        }
    }

    /**
     * Verifica se o método HTTP suporta um corpo (body) de requisição.
     *
     * @param method o método HTTP.
     * @return true se o método suporta corpo, false caso contrário.
     */
    private boolean isBodySupported(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }

    /**
     * Extrai e monta o corpo da requisição a partir do JSON de entrada.
     * <p>
     * Filtra o JSON de entrada para incluir apenas os campos que não são
     * parâmetros de path, query ou header, assumindo que o restante compõe o corpo.
     *
     * @param inputJson o JSON com todos os argumentos.
     * @param params    a lista de parâmetros do endpoint para saber o que filtrar.
     * @return uma string JSON contendo apenas os dados do corpo da requisição.
     */
    private String getRequestBody(JsonNode inputJson, List<OpenApiParameter> params) {
        ObjectNode bodyNode = objectMapper.createObjectNode();
        inputJson.fields().forEachRemaining(entry -> {
            // Se o campo não for um parâmetro de path, query ou header, ele pertence ao corpo.
            if (!isPathQueryOrHeaderParam(entry.getKey(), params)) {
                bodyNode.set(entry.getKey(), entry.getValue());
            }
        });
        return bodyNode.toString();
    }

    /**
     * Verifica se um campo do JSON de entrada é um parâmetro (path, query, header)
     * e, portanto, não faz parte do corpo da requisição.
     *
     * @param fieldName o nome do campo a ser verificado.
     * @param params    a lista de parâmetros do endpoint.
     * @return true se o campo é um parâmetro de path, query ou header, false caso contrário.
     */
    private boolean isPathQueryOrHeaderParam(String fieldName, List<OpenApiParameter> params) {
        return params.stream()
                .anyMatch(p -> p.name().equals(fieldName) &&
                        (PARAM_IN_PATH.equals(p.in()) ||
                                PARAM_IN_QUERY.equals(p.in()) ||
                                PARAM_IN_HEADER.equals(p.in())));
    }

    /**
     * Converte uma lista de {@link DynamicTool} em uma lista de {@link org.springframework.ai.model.function.FunctionCallback}.
     * <p>
     * Isso permite que as ferramentas sejam registradas no ChatClient do Spring AI.
     *
     * @param tools lista de ferramentas dinâmicas.
     * @return lista de callbacks para o modelo de linguagem.
     */
    public List<FunctionCallback> convertToFunctionCallbacks(List<DynamicTool> tools) {
        return tools.stream()
                .map(tool -> FunctionCallbackWrapper.builder(tool.getFunction())
                        .withName(tool.getName())
                        .withDescription(tool.getDescription())
                        // O esquema JSON é fundamental para o modelo saber como formatar a entrada da função.
                        .withInputTypeSchema(tool.getJsonSchema())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Cria uma string JSON de resposta de erro padronizada.
     *
     * @param message    a mensagem de erro.
     * @param statusCode o código de status HTTP do erro.
     * @return uma string JSON representando o erro.
     */
    private String createErrorResponse(String message, int statusCode) {
        try {
            return objectMapper.writeValueAsString(new ToolExecutionResult(statusCode, message));
        } catch (JsonProcessingException e) {
            // Fallback em caso de erro na serialização do erro.
            return "{\"httpStatusCode\":" + statusCode + ",\"body\":\"" + message.replace("\"", "\\\"") + "\"}";
        }
    }
} 
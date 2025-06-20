package com.example.swaggeragent.service.tool;

import com.example.swaggeragent.model.response.ToolExecutionResult;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço responsável por executar chamadas a APIs externas com base em uma definição
 * de endpoint da OpenAPI (representada por {@link OpenApiEndpoint}).
 * <p>
 * Esta classe é o coração da execução de ferramentas dinâmicas. Ela recebe uma representação
 * abstrata de um endpoint e um JSON com os parâmetros, e então:
 * 1. Constrói a URL completa, substituindo variáveis de path e adicionando query params.
 * 2. Monta os cabeçalhos HTTP, incluindo os de segurança e os definidos pela API.
 * 3. Constrói o corpo (body) da requisição, se o método permitir.
 * 4. Utiliza o {@link WebClient} para executar a chamada HTTP de forma reativa.
 * 5. Empacota o resultado (status code e corpo da resposta) em um {@link ToolExecutionResult}.
 *
 * @author cesar schutz
 */
@Service
public class ApiExecutionService implements ToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ApiExecutionService.class);
    private static final String PARAM_IN_PATH = "path";
    private static final String PARAM_IN_QUERY = "query";
    private static final String PARAM_IN_HEADER = "header";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_TRAFFIC_CODE = "Traffic-Code";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiAuthorizationToken;
    private final String apiTrafficCode;

    public ApiExecutionService(WebClient webClient, ObjectMapper objectMapper,
                               @Value("${api.security.authorization.token}") String apiAuthorizationToken,
                               @Value("${api.security.traffic-code}") String apiTrafficCode) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.apiAuthorizationToken = apiAuthorizationToken;
        this.apiTrafficCode = apiTrafficCode;
    }

    @Override
    public ToolExecutionResult execute(OpenApiEndpoint endpoint, String input) {
        return executeEndpoint(endpoint, input);
    }

    /**
     * Executa uma chamada para um endpoint de API definido.
     *
     * @param endpoint A definição do endpoint da OpenAPI a ser chamado.
     * @param input    Uma string JSON contendo os parâmetros para a chamada (para path, query, header e body).
     * @return Um {@link ToolExecutionResult} com o resultado da chamada HTTP.
     */
    public ToolExecutionResult executeEndpoint(OpenApiEndpoint endpoint, String input) {
        try {
            // 1. Faz o parsing do JSON de entrada para um objeto JsonNode para fácil manipulação.
            JsonNode inputJson = objectMapper.readTree(input);

            // 2. Constrói a URL final, substituindo placeholders e adicionando query params.
            String url = buildUrl(endpoint, inputJson);
            HttpMethod method = HttpMethod.valueOf(endpoint.method().toUpperCase());

            // 3. Inicia a construção da requisição com o WebClient.
            WebClient.RequestBodySpec request = webClient
                    .method(method) // Define o método HTTP (GET, POST, etc.)
                    .uri(url)       // Define a URL completa
                    .headers(httpHeaders -> addHeaders(httpHeaders, endpoint, inputJson)); // Adiciona os cabeçalhos

            // 4. Adiciona o corpo (body) à requisição apenas para métodos que o suportam (POST, PUT, PATCH).
            WebClient.ResponseSpec response;
            if (isBodySupported(method)) {
                String requestBody = getRequestBody(inputJson, endpoint.parameters());
                response = request.bodyValue(requestBody).retrieve();
            } else {
                response = request.retrieve();
            }

            // 5. Executa a chamada e processa a resposta.
            // O WebClient é reativo e retorna um Mono/Flux. Como a interface da ferramenta
            // da IA espera uma resposta síncrona, usamos .block() para aguardar o resultado.
            // Em uma aplicação totalmente reativa, evitaríamos o .block().
            return response.toEntity(String.class)
                    .map(entity -> new ToolExecutionResult(entity.getStatusCode().value(), entity.getBody()))
                    .onErrorResume(e -> { // Tratamento de erros na chamada HTTP (ex: falha de conexão).
                        log.error("Erro ao executar chamada para {}: {}", url, e.getMessage());
                        return Mono.just(new ToolExecutionResult(500, createErrorResponse(e.getMessage(), 500)));
                    })
                    .block();

        } catch (JsonProcessingException e) {
            // Captura erros de parsing do JSON de entrada, indicando um problema com os dados fornecidos pela IA.
            log.error("Erro de parsing no JSON de entrada para a ferramenta '{}'", endpoint.operationId(), e);
            return new ToolExecutionResult(400, createErrorResponse("JSON de entrada inválido: " + e.getMessage(), 400));
        } catch (Exception e) {
            // Captura qualquer outro erro inesperado durante o processo.
            log.error("Erro inesperado ao executar a ferramenta '{}'", endpoint.operationId(), e);
            return new ToolExecutionResult(500, createErrorResponse("Erro inesperado: " + e.getMessage(), 500));
        }
    }

    /**
     * Constrói a URL completa para a requisição, incluindo a substituição de
     * parâmetros de path e a adição de parâmetros de query.
     *
     * @param endpoint   O endpoint da API.
     * @param inputJson  O JSON com os valores dos parâmetros.
     * @return A URL final como String.
     */
    private String buildUrl(OpenApiEndpoint endpoint, JsonNode inputJson) {
        String baseUrl = endpoint.baseUrl();
        String path = endpoint.path();

        // Substitui os parâmetros de path (ex: /users/{userId}) pelos valores do JSON.
        for (OpenApiParameter param : endpoint.parameters()) {
            if (PARAM_IN_PATH.equalsIgnoreCase(param.in())) {
                path = path.replace("{" + param.name() + "}", inputJson.get(param.name()).asText());
            }
        }

        // Constrói a string de query (ex: ?name=valor1&status=ativo).
        StringBuilder queryParams = new StringBuilder();
        for (OpenApiParameter param : endpoint.parameters()) {
            if (PARAM_IN_QUERY.equalsIgnoreCase(param.in()) && inputJson.has(param.name())) {
                if (queryParams.length() == 0) {
                    queryParams.append("?");
                } else {
                    queryParams.append("&");
                }
                queryParams.append(param.name()).append("=").append(inputJson.get(param.name()).asText());
            }
        }

        return baseUrl + path + queryParams;
    }

    /**
     * Adiciona os cabeçalhos à requisição HTTP.
     * Inclui cabeçalhos fixos (Content-Type, Auth) e os dinâmicos vindos do JSON de entrada.
     */
    private void addHeaders(HttpHeaders httpHeaders, OpenApiEndpoint endpoint, JsonNode inputJson) {
        httpHeaders.add("Content-Type", "application/json");
        // Adiciona cabeçalhos de segurança configurados na aplicação.
        if (apiAuthorizationToken != null && !apiAuthorizationToken.isBlank()) {
            httpHeaders.add(HEADER_AUTHORIZATION, "Bearer " + apiAuthorizationToken);
        }
        if (apiTrafficCode != null && !apiTrafficCode.isBlank()) {
            httpHeaders.add(HEADER_TRAFFIC_CODE, apiTrafficCode);
        }

        // Adiciona cabeçalhos dinâmicos definidos na especificação da API.
        for (OpenApiParameter param : endpoint.parameters()) {
            if (PARAM_IN_HEADER.equalsIgnoreCase(param.in()) && inputJson.has(param.name())) {
                httpHeaders.add(param.name(), inputJson.get(param.name()).asText());
            }
        }
    }

    /**
     * Verifica se o método HTTP suporta o envio de um corpo (body).
     */
    private boolean isBodySupported(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }

    /**
     * Extrai do JSON de entrada apenas os campos que devem compor o corpo da requisição,
     * ignorando os que já foram usados como parâmetros de path, query ou header.
     *
     * @param inputJson O JSON completo fornecido pela IA.
     * @param params    A lista de todos os parâmetros definidos para o endpoint.
     * @return Uma string JSON contendo apenas os campos do corpo da requisição.
     */
    private String getRequestBody(JsonNode inputJson, List<OpenApiParameter> params) {
        // Identifica os nomes de todos os parâmetros que *não* são para o corpo da requisição.
        List<String> nonBodyParamNames = params.stream()
                .filter(p -> PARAM_IN_PATH.equalsIgnoreCase(p.in()) ||
                              PARAM_IN_QUERY.equalsIgnoreCase(p.in()) ||
                              PARAM_IN_HEADER.equalsIgnoreCase(p.in()))
                .map(OpenApiParameter::name)
                .collect(Collectors.toList());

        // Cria um novo objeto JSON (bodyNode) e copia para ele apenas os campos do JSON de entrada
        // que não estão na lista de parâmetros "não-corpo".
        ObjectNode bodyNode = objectMapper.createObjectNode();
        inputJson.fields().forEachRemaining(entry -> {
            if (!nonBodyParamNames.contains(entry.getKey())) {
                bodyNode.set(entry.getKey(), entry.getValue());
            }
        });

        try {
            return objectMapper.writeValueAsString(bodyNode);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar o corpo da requisição.", e);
            return "{}"; // Retorna um corpo vazio em caso de erro.
        }
    }

    /**
     * Cria uma string JSON padronizada para respostas de erro.
     */
    private String createErrorResponse(String message, int statusCode) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "error", message,
                    "status", statusCode
            ));
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Falha ao serializar a resposta de erro.\"}";
        }
    }
} 
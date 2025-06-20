package com.example.swaggeragent.service.tool;

import com.example.swaggeragent.model.domain.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiParameter;
import com.example.swaggeragent.model.OpenApiRequestBody;
import com.example.swaggeragent.model.OpenApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.swaggeragent.service.parser.OpenApiParserService;

/**
 * Serviço dedicado a registrar (fazer "log") de forma detalhada as informações
 * sobre as ferramentas dinâmicas que foram geradas e carregadas.
 * <p>
 * O principal objetivo deste serviço é fornecer uma saída clara e estruturada no console
 * durante a inicialização da aplicação. Isso ajuda os desenvolvedores a verificar
 * rapidamente quais ferramentas estão ativas, seus nomes, parâmetros e estruturas
 * de dados, facilitando a depuração e o desenvolvimento.
 */
@Service
public class ToolLoggerService {

    private static final Logger log = LoggerFactory.getLogger(ToolLoggerService.class);
    private final OpenApiParserService openApiParserService;

    /**
     * Construtor para injeção de dependências.
     *
     * @param objectMapper         o mapper JSON para serialização.
     * @param openApiParserService o serviço de análise OpenAPI, usado para obter esquemas JSON.
     */
    public ToolLoggerService(OpenApiParserService openApiParserService) {
        this.openApiParserService = openApiParserService;
    }

    /**
     * Itera sobre uma lista de {@link DynamicTool} e registra os detalhes de cada uma.
     * <p>
     * Apresenta as informações em um formato legível, com seções para nome, descrição,
     * entradas (parâmetros e corpo da requisição) e saídas (respostas possíveis).
     *
     * @param tools a lista de ferramentas dinâmicas a serem registradas.
     */
    public void logTools(List<DynamicTool> tools) {
        if (tools == null || tools.isEmpty()) {
            log.info("Nenhuma ferramenta dinâmica para registrar.");
            return;
        }

        log.info("\n====================================================================================================");
        log.info("                           🛠️  Ferramentas Dinâmicas Carregadas 🛠️");
        log.info("====================================================================================================");

        for (int i = 0; i < tools.size(); i++) {
            DynamicTool tool = tools.get(i);
            OpenApiEndpoint endpoint = tool.getEndpoint();

            log.info("\n----------------------------------------- Ferramenta {} de {} --------------------------------------------", i + 1, tools.size());
            log.info("🏷️  Nome: {}", tool.getName());
            log.info("📝 Descrição: {}", tool.getDescription());
            log.info("🆔 ID da Operação: {}", endpoint.operationId());
            log.info("🌐 Projeto: {}", endpoint.projectName());
            log.info("🎯 Controller: {}", endpoint.tags() != null && !endpoint.tags().isEmpty() ? endpoint.tags().get(0) : "general");
            log.info("   {} {}{}", endpoint.method().toUpperCase(), endpoint.baseUrl(), endpoint.path());
            log.info("------------------------------------------- ENTRADAS ---------------------------------------------");

            logParameters(endpoint.parameters());
            logRequestBody(endpoint);

            log.info("------------------------------------------- SAÍDAS --------------------------------------------");
            logResponses(endpoint);

            log.info("----------------------------------------------------------------------------------------------------");
        }
        log.info("\n====================================== {} Ferramentas Carregadas com Sucesso ======================================\n", tools.size());
    }

    /**
     * Registra os parâmetros de um endpoint, agrupando-os por localização (path, query, header).
     *
     * @param parameters a lista de parâmetros do endpoint.
     */
    private void logParameters(List<OpenApiParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            log.info("  Nenhum parâmetro de entrada definido.");
            return;
        }

        // Agrupa os parâmetros para uma exibição mais organizada.
        Map<String, List<OpenApiParameter>> groupedParameters = parameters.stream()
                .collect(Collectors.groupingBy(OpenApiParameter::in));

        logGroupedParameters("Path", groupedParameters.get("path"));
        logGroupedParameters("Query", groupedParameters.get("query"));
        logGroupedParameters("Header", groupedParameters.get("header"));
    }

    /**
     * Registra um grupo específico de parâmetros (ex: todos os parâmetros de "Query").
     *
     * @param groupName o nome do grupo a ser exibido (ex: "Path", "Query").
     * @param params    a lista de parâmetros pertencentes a esse grupo.
     */
    private void logGroupedParameters(String groupName, List<OpenApiParameter> params) {
        if (params != null && !params.isEmpty()) {
            log.info("  📥 Parâmetros {}:", groupName);
            for (OpenApiParameter param : params) {
                String requiredInfo = param.required() ? "OBRIGATÓRIO" : "OPCIONAL";
                log.info("     - {} ({}) [{}]: {}", param.name(), param.type(), requiredInfo, param.description());

                if (param.format() != null) {
                    log.info("       Formato: {}", param.format());
                }
                if (param.defaultValue() != null) {
                    log.info("       Padrão: {}", param.defaultValue());
                }
                if (param.enumValues() != null && !param.enumValues().isEmpty()) {
                    log.info("       Valores Enum: {}", param.enumValues());
                }
                if (param.items() != null) {
                    log.info("       Tipo dos Items: {}", param.items().type());
                }
            }
        }
    }

    /**
     * Registra os detalhes do corpo da requisição (request body) de um endpoint.
     *
     * @param endpoint o endpoint da API.
     */
    private void logRequestBody(OpenApiEndpoint endpoint) {
        OpenApiRequestBody requestBody = endpoint.requestBody();
        if (requestBody != null && requestBody.content() != null && !requestBody.content().isEmpty()) {
            log.info("  📦 Corpo da Requisição:");
            log.info("     - Obrigatório: {}", requestBody.required() ? "Sim" : "Não");
            if (requestBody.description() != null && !requestBody.description().isBlank()) {
                log.info("     - Descrição: {}", requestBody.description());
            }
            requestBody.content().forEach((mediaType, mediaTypeObject) -> {
                log.info("     - Tipo: `{}`", mediaType);
                if (mediaTypeObject.schema() != null) {
                    String schemaJson = openApiParserService.getSchemaAsJson(endpoint.projectName(), mediaTypeObject.schema());
                    log.info("     - Schema:");
                    // Adiciona uma indentação ao JSON para melhor legibilidade no log.
                    log.info("\n{}",schemaJson.indent(6));
                }
            });
        }
    }

    /**
     * Registra os detalhes das possíveis respostas de um endpoint.
     *
     * @param endpoint o endpoint da API.
     */
    private void logResponses(OpenApiEndpoint endpoint) {
        Map<String, OpenApiResponse> responses = endpoint.responses();
        if (responses == null || responses.isEmpty()) {
            log.info("  Nenhuma resposta de saída definida.");
            return;
        }
        log.info("  📤 Respostas Possíveis:");
        responses.forEach((statusCode, response) -> {
            log.info("    - **`{}`**: {}", statusCode, response.description());
            if (response.content() != null) {
                response.content().forEach((mediaType, mediaTypeObject) -> {
                    if (mediaTypeObject.schema() != null) {
                        log.info("      - Schema (`{}`):", mediaType);
                        String schemaJson = openApiParserService.getSchemaAsJson(endpoint.projectName(), mediaTypeObject.schema());
                        // Adiciona uma indentação ao JSON para melhor legibilidade no log.
                        log.info("\n{}", schemaJson.indent(8));

                    }
                    if (mediaTypeObject.example() != null) {
                        log.info("      Exemplo: {}", mediaTypeObject.example().toString());
                    }
                });
            }
        });
    }
} 
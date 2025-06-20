package com.example.swaggeragent.model;

import java.util.List;
import java.util.Map;

/**
 * Representa um endpoint de API extraído de uma especificação OpenAPI.
 * <p>
 * Este record imutável contém todas as informações relevantes sobre um único endpoint,
 * como seu ID de operação, método HTTP, caminho, parâmetros, corpo da requisição e respostas.
 *
 * @param operationId o identificador único da operação (operationId).
 * @param method      o método HTTP do endpoint (ex: "get", "post").
 * @param path        o caminho do endpoint (ex: "/users/{id}").
 * @param summary     um resumo curto da operação.
 * @param description uma descrição detalhada da operação.
 * @param baseUrl     a URL base do servidor da API.
 * @param projectName o nome do projeto ao qual o endpoint pertence.
 * @param parameters  a lista de parâmetros que o endpoint aceita.
 * @param requestBody o corpo da requisição esperado pelo endpoint.
 * @param responses   um mapa das possíveis respostas da API.
 * @param tags        uma lista de tags para agrupar operações.
 */
public record OpenApiEndpoint(
        String operationId,
        String method,
        String path,
        String summary,
        String description,
        String baseUrl,
        String projectName,
        List<OpenApiParameter> parameters,
        OpenApiRequestBody requestBody,
        Map<String, OpenApiResponse> responses,
        List<String> tags
) {
} 
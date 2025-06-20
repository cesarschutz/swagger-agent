package com.example.swaggeragent.model;

import java.util.Map;
import io.swagger.v3.oas.models.headers.Header;

/**
 * Representa uma resposta de uma operação de API na especificação OpenAPI.
 * <p>
 * Contém a descrição da resposta e um mapa dos tipos de mídia que ela pode retornar.
 *
 * @param description uma descrição do que esta resposta significa.
 * @param content     um mapa onde a chave é o tipo de mídia (ex: "application/json")
 *                    e o valor é um {@link OpenApiMediaType} que descreve o conteúdo da resposta.
 * @param headers     um mapa dos headers que podem ser retornados nesta resposta.
 */
public record OpenApiResponse(
        String description,
        Map<String, OpenApiMediaType> content,
        Map<String, Header> headers
) {
} 
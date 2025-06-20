package com.example.swaggeragent.model;

import java.util.Map;

/**
 * Representa o corpo (body) de uma requisição em uma operação OpenAPI.
 * <p>
 * Contém a descrição, a obrigatoriedade e um mapa dos tipos de mídia
 * que o corpo da requisição pode aceitar.
 *
 * @param description uma descrição do corpo da requisição.
 * @param required    indica se o corpo da requisição é obrigatório.
 * @param content     um mapa onde a chave é o tipo de mídia (ex: "application/json")
 *                    e o valor é um {@link OpenApiMediaType} que descreve o conteúdo.
 */
public record OpenApiRequestBody(
        String description,
        boolean required,
        Map<String, OpenApiMediaType> content
) {
} 
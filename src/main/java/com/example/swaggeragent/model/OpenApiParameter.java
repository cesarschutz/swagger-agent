package com.example.swaggeragent.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Representa um parâmetro de uma operação de API, conforme definido na especificação OpenAPI.
 * <p>
 * Inclui informações como nome, localização (in), tipo, obrigatoriedade, e mais.
 * A anotação {@code @JsonInclude(JsonInclude.Include.NON_NULL)} garante que campos nulos
 * não sejam serializados para JSON, mantendo a definição do esquema limpa.
 *
 * @param name         o nome do parâmetro.
 * @param in           a localização do parâmetro (ex: "query", "header", "path").
 * @param description  uma descrição do que o parâmetro representa.
 * @param required     indica se o parâmetro é obrigatório.
 * @param type         o tipo de dado do parâmetro (ex: "string", "integer").
 * @param format       um formato mais específico para o tipo de dado (ex: "int64", "date-time").
 * @param defaultValue o valor padrão do parâmetro, se houver.
 * @param enumValues   uma lista de valores permitidos para o parâmetro, se for um enum.
 * @param items        a definição dos itens, caso o parâmetro seja um array.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenApiParameter(
        String name,
        String in,
        String description,
        boolean required,
        String type,
        String format,
        Object defaultValue,
        List<String> enumValues,
        OpenApiParameterItems items
) {
} 
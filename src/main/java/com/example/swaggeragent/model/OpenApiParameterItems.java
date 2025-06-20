package com.example.swaggeragent.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representa a definição dos itens de um parâmetro do tipo array em uma especificação OpenAPI.
 * <p>
 * Descreve o tipo e o formato dos elementos contidos em um array.
 * A anotação {@code @JsonInclude(JsonInclude.Include.NON_NULL)} é usada para omitir
 * campos nulos na serialização JSON.
 *
 * @param type   o tipo de dado dos itens no array (ex: "string", "integer").
 * @param format um formato mais específico para o tipo de dado (ex: "int64").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenApiParameterItems(
        String type,
        String format
) {
} 
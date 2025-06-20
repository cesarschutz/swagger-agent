package com.example.swaggeragent.model;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;

/**
 * Representa o tipo de mídia para uma requisição ou resposta OpenAPI.
 * <p>
 * Este record contém o esquema (schema) que define a estrutura dos dados,
 * um exemplo único e um mapa de exemplos nomeados.
 * É comumente usado para descrever o conteúdo de corpos de requisição (request bodies)
 * ou respostas (responses), como 'application/json'.
 *
 * @param schema   o objeto {@link Schema} que define a estrutura de dados.
 * @param example  um exemplo único e não nomeado do payload.
 * @param examples um mapa de exemplos nomeados do payload.
 */
public record OpenApiMediaType(
        Schema schema,
        Object example,
        Map<String, Example> examples
) {
} 
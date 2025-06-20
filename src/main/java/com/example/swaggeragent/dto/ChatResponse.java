package com.example.swaggeragent.dto;

/**
 * DTO (Data Transfer Object) que representa a resposta de uma mensagem de chat.
 * <p>
 * Este record imutável contém o conteúdo da resposta gerada pelo modelo de linguagem
 * e o papel (role) do autor da mensagem.
 *
 * @param role    o papel do autor da mensagem (ex: "assistant", "user", "system")
 * @param content o conteúdo textual da resposta
 */
public record ChatResponse(String role, String content) {
} 
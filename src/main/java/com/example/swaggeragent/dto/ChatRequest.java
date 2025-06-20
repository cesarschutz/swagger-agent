package com.example.swaggeragent.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO (Data Transfer Object) que representa a requisição de uma mensagem de chat.
 * <p>
 * Contém a mensagem do usuário e um ID de sessão opcional para manter o contexto
 * da conversa. A mensagem é obrigatória.
 */
public class ChatRequest {

    /**
     * A mensagem enviada pelo usuário. Não pode ser nula ou vazia.
     */
    @NotBlank(message = "A mensagem não pode estar em branco")
    private String message;

    /**
     * O ID da sessão para rastrear o histórico da conversa. Pode ser nulo.
     */
    private String sessionId;

    /**
     * Construtor padrão.
     */
    public ChatRequest() {
    }

    /**
     * Construtor que recebe a mensagem como parâmetro.
     *
     * @param message a mensagem do usuário.
     */
    public ChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
} 
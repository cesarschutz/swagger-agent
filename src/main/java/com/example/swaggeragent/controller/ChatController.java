package com.example.swaggeragent.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import com.example.swaggeragent.dto.ChatRequest;
import com.example.swaggeragent.dto.ChatResponse;
import com.example.swaggeragent.service.chat.ChatService;

/**
 * Controller responsável por gerenciar as interações de chat com o usuário.
 * <p>
 * Este controlador expõe endpoints para comunicação síncrona e assíncrona (streaming)
 * com o serviço de chat. Ele delega a lógica de processamento da mensagem para o {@link ChatService}.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * Construtor para injeção de dependência do {@link ChatService}.
     *
     * @param chatService o serviço de chat a ser injetado.
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Endpoint para uma interação de chat síncrona.
     * <p>
     * Recebe uma pergunta do usuário, processa e retorna a resposta completa da IA.
     * Utiliza o método POST e espera um {@link ChatRequest} no corpo da requisição.
     *
     * @param request o objeto {@link ChatRequest} contendo a mensagem e o ID da sessão.
     * @return um {@link ChatResponse} com a resposta completa do modelo de linguagem.
     */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.processChatMessage(request.getMessage(), request.getSessionId());
    }

    /**
     * Endpoint para uma interação de chat assíncrona com streaming via Server-Sent Events (SSE).
     * <p>
     * Permite que o cliente receba a resposta da IA em partes (chunks) em tempo real,
     * melhorando a percepção de responsividade.
     * Produz um fluxo de eventos do tipo {@code text/event-stream}.
     *
     * @param request o objeto {@link ChatRequest} contendo a mensagem e o ID da sessão.
     * @return um {@link Flux} de {@link String} onde cada elemento é um pedaço da resposta.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChatResponse(request.getMessage(), request.getSessionId());
    }
} 
package com.example.swaggeragent.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import com.example.swaggeragent.dto.ChatRequest;
import com.example.swaggeragent.dto.ChatResponse;
import com.example.swaggeragent.service.ChatService;

/**
 * Controller responsável por gerenciar as interações de chat.
 * Suporta tanto chat síncrono quanto streaming assíncrono.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Permite CORS para desenvolvimento
public class ChatController {
    
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Endpoint de chat síncrono (compatibilidade com versão anterior).
     * 
     * @param request Requisição de chat
     * @return Resposta completa do chat
     */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        System.out.println("---> Chat síncrono iniciado");
        return chatService.chat(request.getMessage(), "123");
    }

    /**
     * Endpoint de chat com streaming usando Server-Sent Events.
     * Permite receber a resposta da IA em tempo real conforme é gerada.
     * 
     * @param request Requisição de chat
     * @return Flux de chunks da resposta
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        System.out.println("---> Chat streaming iniciado");
        return chatService.chatStream(request.getMessage(), "123");
    }
} 
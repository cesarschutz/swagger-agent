package com.example.springialocal.application.controller;

import com.example.springialocal.application.dto.ChatRequest;
import com.example.springialocal.application.dto.ChatResponse;
import com.example.springialocal.domain.service.ChatService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.generateResponse(request.getMessage());
    }
}
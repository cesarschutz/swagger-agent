package com.example.springialocal.controller;

import com.example.springialocal.dto.ChatRequest;
import com.example.springialocal.dto.ChatResponse;
import com.example.springialocal.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        System.out.println("---> controler");
        return chatService.chat(request.getMessage(), "123");
    }
} 
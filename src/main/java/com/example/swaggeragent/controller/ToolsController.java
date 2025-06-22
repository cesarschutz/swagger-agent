package com.example.swaggeragent.controller;

import org.springframework.web.bind.annotation.*;
import com.example.swaggeragent.dto.ToolsResponse;
import com.example.swaggeragent.service.chat.ChatService;

@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    private final ChatService chatService;

    public ToolsController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ToolsResponse getAvailableTools() {
        return new ToolsResponse(chatService.getAvailableTools());
    }
}
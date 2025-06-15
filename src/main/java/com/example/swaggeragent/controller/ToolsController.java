package com.example.swaggeragent.controller;

import org.springframework.web.bind.annotation.*;
import com.example.swaggeragent.dto.ToolsResponse;
import com.example.swaggeragent.service.ChatService;

/**
 * Controller responsável por fornecer informações sobre as ferramentas disponíveis.
 */
@RestController
@RequestMapping("/api/tools")
@CrossOrigin(origins = "*")
public class ToolsController {
    
    private final ChatService chatService;

    public ToolsController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Retorna todas as ferramentas disponíveis organizadas por projeto.
     * 
     * @return Resposta com informações das ferramentas
     */
    @GetMapping
    public ToolsResponse getAvailableTools() {
        return new ToolsResponse(chatService.getCurrentTools());
    }
}


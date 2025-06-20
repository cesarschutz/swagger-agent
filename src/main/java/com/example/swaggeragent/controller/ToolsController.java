package com.example.swaggeragent.controller;

import org.springframework.web.bind.annotation.*;
import com.example.swaggeragent.dto.ToolsResponse;
import com.example.swaggeragent.service.chat.ChatService;

/**
 * Controller responsável por expor informações sobre as ferramentas (tools) dinâmicas
 * que foram geradas e estão disponíveis para uso pelo modelo de linguagem.
 */
@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    private final ChatService chatService;

    /**
     * Construtor para injeção de dependência do {@link ChatService}.
     *
     * @param chatService o serviço de chat que gerencia as ferramentas.
     */
    public ToolsController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Endpoint para obter a lista de todas as ferramentas atualmente disponíveis.
     * <p>
     * As ferramentas são geradas dinamicamente a partir de especificações OpenAPI
     * e este endpoint permite que clientes ou desenvolvedores inspecionem
     * quais ferramentas estão ativas na sessão atual.
     *
     * @return um {@link ToolsResponse} contendo a lista de ferramentas disponíveis.
     */
    @GetMapping
    public ToolsResponse getAvailableTools() {
        return new ToolsResponse(chatService.getAvailableTools());
    }
}
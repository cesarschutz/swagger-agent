package com.example.swaggeragent.service.chat;

import com.example.swaggeragent.service.audit.AuditService;
import com.example.swaggeragent.service.parser.OpenApiParserService;
import com.example.swaggeragent.service.SystemPromptService;
import com.example.swaggeragent.service.tool.DynamicToolGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários para o {@link ChatService}.
 * <p>
 * Esta classe testa o comportamento do serviço de chat de forma isolada,
 * mockando todas as dependências externas para garantir que os testes
 * sejam determinísticos e rápidos.
 */
class ChatServiceTest {
    
    @Mock private ChatModel chatModel;
    @Mock private OpenApiParserService openApiParserService;
    @Mock private DynamicToolGeneratorService dynamicToolGeneratorService;
    @Mock private SystemPromptService systemPromptService;
    @Mock private ChatMemoryService chatMemoryService;
    @Mock private AuditService auditService;

    private ChatService chatService;

    /**
     * Configura o ambiente de teste antes de cada método de teste.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        chatService = new ChatService(
                chatModel,
                openApiParserService,
                dynamicToolGeneratorService,
                systemPromptService,
                chatMemoryService,
                auditService
        );
    }

    /**
     * Testa o comportamento do ChatService com mensagem inválida.
     */
    @Test
    void testProcessChatMessage_invalidMessage_throwsException() {
        String sessionId = "sessao1";
        assertThrows(RuntimeException.class, () -> chatService.processChatMessage("", sessionId));
    }

    /**
     * Testa o comportamento do ChatService com sessionId inválido.
     */
    @Test
    void testProcessChatMessage_invalidSessionId_throwsException() {
        String message = "Olá";
        assertThrows(RuntimeException.class, () -> chatService.processChatMessage(message, " "));
    }
} 
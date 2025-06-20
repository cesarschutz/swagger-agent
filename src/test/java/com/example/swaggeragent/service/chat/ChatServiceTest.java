package com.example.swaggeragent.service.chat;

import com.example.swaggeragent.service.parser.OpenApiParserService;
import com.example.swaggeragent.service.tool.DynamicToolGeneratorService;
import com.example.swaggeragent.service.SystemPromptService;
import com.example.swaggeragent.service.tool.ToolLoggerService;
import com.example.swaggeragent.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o {@link ChatService}.
 * <p>
 * Esta classe de teste utiliza mocks para isolar o ChatService e testar
 * sua lógica de negócio de forma independente das dependências externas.
 */
class ChatServiceTest {
    
    @Mock private ChatModel chatModel;
    @Mock private OpenApiParserService openApiParserService;
    @Mock private DynamicToolGeneratorService dynamicToolGeneratorService;
    @Mock private SystemPromptService systemPromptService;
    @Mock private ToolLoggerService toolLoggerService;
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
                toolLoggerService,
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
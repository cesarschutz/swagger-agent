package com.example.swaggeragent.service.chat;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import com.example.swaggeragent.dto.ChatResponse;
import com.example.swaggeragent.model.domain.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.service.parser.OpenApiParserService;
import com.example.swaggeragent.service.tool.DynamicToolGeneratorService;
import com.example.swaggeragent.service.SystemPromptService;
import com.example.swaggeragent.service.audit.AuditService;

import java.time.Instant;
import java.util.List;

/**
 * Serviço central que orquestra toda a lógica de chat e o gerenciamento de ferramentas (tools).
 * <p>
 * Responsabilidades principais:
 * <ul>
 *   <li>Inicializar e configurar o cliente de chat ({@link ChatClient}) com as ferramentas dinâmicas.</li>
 *   <li>Carregar, analisar e converter especificações OpenAPI em ferramentas executáveis.</li>
 *   <li>Gerenciar o ciclo de vida das conversas, incluindo a memória de chat por sessão.</li>
 *   <li>Processar requisições de chat síncronas e assíncronas (streaming SSE).</li>
 *   <li>Fornecer acesso às ferramentas atualmente carregadas.</li>
 *   <li>Registrar auditoria e eventos de segurança.</li>
 * </ul>
 *
 * <b>Fluxo principal:</b>
 * <ol>
 *   <li>Ao iniciar, carrega todas as especificações OpenAPI e gera as ferramentas dinâmicas.</li>
 *   <li>Configura o ChatClient com o prompt de sistema e as funções (tools) disponíveis.</li>
 *   <li>Ao receber uma mensagem, valida os parâmetros, recupera a memória da sessão e envia para o modelo de IA.</li>
 *   <li>Registra logs de auditoria e eventos de segurança para cada interação.</li>
 * </ol>
 *
 * <b>Dica para devs juniores:</b> Leia os comentários de cada método para entender o papel de cada etapa.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // Dependências injetadas para o funcionamento do serviço.
    private final ChatModel chatModel;
    private final OpenApiParserService openApiParserService;
    private final DynamicToolGeneratorService dynamicToolGeneratorService;
    private final SystemPromptService systemPromptService;
    private final ChatMemoryService chatMemoryService;
    private final AuditService auditService;

    /**
     * O cliente de chat configurado para interagir com o modelo de linguagem.
     */
    private ChatClient chatClient;
    /**
     * A lista de ferramentas dinâmicas que foram geradas a partir das especificações OpenAPI.
     */
    private List<DynamicTool> availableTools;

    /**
     * Construtor para injeção de todas as dependências necessárias.
     *
     * @param chatModel                  o modelo de chat (OpenAI ou Ollama).
     * @param openApiParserService       o serviço para analisar arquivos OpenAPI.
     * @param dynamicToolGeneratorService o serviço para criar ferramentas dinâmicas.
     * @param systemPromptService        o serviço para gerar o prompt de sistema.
     * @param chatMemoryService          o serviço para gerenciar memória de chat.
     * @param auditService               o serviço de auditoria.
     */
    public ChatService(
            ChatModel chatModel,
            OpenApiParserService openApiParserService,
            DynamicToolGeneratorService dynamicToolGeneratorService,
            SystemPromptService systemPromptService,
            ChatMemoryService chatMemoryService,
            AuditService auditService) {
        this.chatModel = chatModel;
        this.openApiParserService = openApiParserService;
        this.dynamicToolGeneratorService = dynamicToolGeneratorService;
        this.systemPromptService = systemPromptService;
        this.chatMemoryService = chatMemoryService;
        this.auditService = auditService;
    }

    /**
     * Método de inicialização invocado após a construção do bean.
     * Dispara o processo de carregamento de ferramentas e configuração do cliente de chat.
     */
    @PostConstruct
    public void initialize() {
        initializeToolRegistry();
    }

    /**
     * Orquestra o carregamento das ferramentas e a inicialização do cliente de chat.
     */
    public void initializeToolRegistry() {
        try {
            log.info("\n" +
                    "╔══════════════════════════════════════════════════════════════════════════════╗\n" +
                    "║                 🚀 INICIALIZANDO SWAGGER AGENT                               ║\n" +
                    "╚══════════════════════════════════════════════════════════════════════════════╝");

            List<OpenApiEndpoint> endpoints = openApiParserService.parseAllOpenApiFiles();
            availableTools = dynamicToolGeneratorService.generateToolsFromEndpoints(endpoints);

            log.info("\n" +
                    "╔══════════════════════════════════════════════════════════════════════════════╗\n" +
                    "║                      🤖 CONFIGURANDO AGENTE IA                               ║\n" +
                    "╚══════════════════════════════════════════════════════════════════════════════╝");

            initializeChatClient();

            String toolsLine = String.format("  🎯 Total de ferramentas: %d", availableTools.size());
            String endpointsLine = String.format("  🌐 Endpoints disponíveis: %d", endpoints.size());
            String agentLine = "  🤖 Agente IA: Configurado e pronto para uso";

            log.info("\n" +
                    "╔══════════════════════════════════════════════════════════════════════════════╗\n" +
                    "║                   ✅ INICIALIZAÇÃO CONCLUÍDA                                 ║\n" +
                    "║                                                                              ║\n" +
                    "║{}" + "║\n" +
                    "║{}" + "║\n" +
                    "║{}" + "║\n" +
                    "╚══════════════════════════════════════════════════════════════════════════════╝",
                    String.format("%-71s", toolsLine),
                    String.format("%-71s", endpointsLine),
                    String.format("%-71s", agentLine)
            );

        } catch (Exception e) {
            log.error("❌ Erro crítico durante a inicialização do Swagger Agent. O serviço pode não funcionar como esperado.", e);
            throw new RuntimeException("Falha na inicialização do serviço de chat", e);
        }
    }

    /**
     * Configura e constrói a instância do {@link ChatClient}.
     */
    private void initializeChatClient() {
        String systemPrompt = systemPromptService.generateSystemPrompt();
        
        List<FunctionCallback> functionCallbacks = dynamicToolGeneratorService.convertToFunctionCallbacks(availableTools);
        log.info("🔧 {} function callbacks registradas e prontas para uso.", functionCallbacks.size());

        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultSystem(systemPrompt);

        if (!functionCallbacks.isEmpty()) {
            builder.defaultFunctions(functionCallbacks.toArray(new FunctionCallback[0]));
        }

        chatClient = builder.build();
        log.info("✅ ChatClient configurado e pronto para receber requisições.");
        log.debug("ChatClient inicializado com o prompt de sistema e {} ferramentas.", functionCallbacks.size());
    }

    /**
     * Processa uma mensagem de chat de forma síncrona.
     * <p>
     * Utiliza a memória de chat associada ao {@code sessionId} para manter o contexto
     * e retorna a resposta completa do modelo de linguagem.
     *
     * <b>Fluxo resumido:</b>
     * <ol>
     *   <li>Valida os parâmetros de entrada.</li>
     *   <li>Recupera ou cria a memória de chat da sessão.</li>
     *   <li>Envia a mensagem para o modelo de IA via ChatClient.</li>
     *   <li>Registra a interação para auditoria.</li>
     *   <li>Retorna a resposta da IA.</li>
     * </ol>
     *
     * @param message   a mensagem enviada pelo usuário.
     * @param sessionId o ID da sessão para rastrear o histórico da conversa.
     * @return um {@link ChatResponse} contendo a resposta do assistente.
     */
    public ChatResponse processChatMessage(String message, String sessionId) {
        validateChatRequest(message, sessionId);
        
        final String role = "assistant";
        Instant startTime = Instant.now();

        if (chatClient == null) {
            log.warn("Tentativa de chat com cliente não inicializado para a sessão: {}", sessionId);
            throw new RuntimeException("O serviço de chat não foi inicializado corretamente");
        }

        try {
            InMemoryChatMemory chatMemory = chatMemoryService.getOrCreate(sessionId);

            // Envia a mensagem do usuário para o modelo, usando um advisor para gerenciar a memória.
            String response = chatClient.prompt()
                    .advisors(new MessageChatMemoryAdvisor(chatMemory))
                    .user(message)
                    .call()
                    .content();

            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            
            // Registra a interação para auditoria
            auditService.logChatInteraction(sessionId, message, response, durationMs);

            log.debug("Resposta de chat síncrono gerada para a sessão: {}", sessionId);

            return new ChatResponse(role, response);

        } catch (Exception e) {
            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            auditService.logSecurityEvent("CHAT_ERROR", sessionId, "Erro ao processar mensagem: " + e.getMessage());
            
            log.error("Erro ao processar mensagem de chat para a sessão: {}", sessionId, e);
            throw new RuntimeException("Erro ao processar mensagem de chat", e);
        }
    }

    /**
     * Processa uma mensagem de chat de forma assíncrona, utilizando streaming (Server-Sent Events).
     * <p>
     * Retorna a resposta em pedaços (chunks) à medida que são gerados pelo modelo,
     * permitindo uma experiência mais reativa para o usuário.
     *
     * <b>Fluxo resumido:</b>
     * <ol>
     *   <li>Valida os parâmetros de entrada.</li>
     *   <li>Recupera ou cria a memória de chat da sessão.</li>
     *   <li>Envia a mensagem para o modelo de IA via ChatClient em modo streaming.</li>
     *   <li>Registra eventos de auditoria e segurança.</li>
     *   <li>Retorna cada pedaço da resposta como um evento SSE.</li>
     * </ol>
     *
     * @param message   a mensagem enviada pelo usuário.
     * @param sessionId o ID da sessão para rastrear o histórico da conversa.
     * @return um {@link Flux} de Strings, onde cada string é um evento SSE.
     */
    public Flux<String> streamChatResponse(String message, String sessionId) {
        validateChatRequest(message, sessionId);
        
        if (chatClient == null) {
            log.warn("Tentativa de chat stream com cliente não inicializado para a sessão: {}", sessionId);
            return Flux.just("data: Erro: O serviço de chat não foi inicializado corretamente. Verifique os logs do servidor.\\n\\n");
        }

        try {
            InMemoryChatMemory chatMemory = chatMemoryService.getOrCreate(sessionId);

            log.debug("Iniciando chat streaming para a sessão: {}", sessionId);

            // Retorna o fluxo (Flux) de respostas, formatando cada chunk como um Server-Sent Event (SSE).
            return chatClient.prompt()
                    .advisors(new MessageChatMemoryAdvisor(chatMemory))
                    .user(message)
                    .stream()
                    .content()
                    .map(chunk -> "data: " + chunk + "\\n\\n")
                    .doOnComplete(() -> {
                        auditService.logChatInteraction(sessionId, message, "[STREAMING_COMPLETED]", 0);
                    })
                    .onErrorResume(e -> {
                        log.error("Erro durante streaming para a sessão: {}", sessionId, e);
                        auditService.logSecurityEvent("STREAMING_ERROR", sessionId, "Erro durante streaming: " + e.getMessage());
                        return Flux.just("data: Erro durante o streaming: " + e.getMessage() + "\\n\\n");
                    });

        } catch (Exception e) {
            log.error("Erro ao iniciar streaming para a sessão: {}", sessionId, e);
            auditService.logSecurityEvent("STREAMING_INIT_ERROR", sessionId, "Erro ao iniciar streaming: " + e.getMessage());
            return Flux.just("data: Erro ao iniciar streaming: " + e.getMessage() + "\\n\\n");
        }
    }

    /**
     * Valida os parâmetros de entrada para requisições de chat.
     * <p>
     * Garante que a mensagem e o sessionId não estejam vazios ou nulos.
     *
     * @param message   a mensagem a ser validada
     * @param sessionId o ID da sessão a ser validado
     */
    private void validateChatRequest(String message, String sessionId) {
        if (message == null || message.trim().isEmpty()) {
            throw new RuntimeException("A mensagem não pode estar vazia");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new RuntimeException("O ID da sessão não pode estar vazio");
        }
    }

    /**
     * Obtém a lista de ferramentas atualmente disponíveis.
     *
     * @return a lista de ferramentas dinâmicas.
     */
    public List<DynamicTool> getAvailableTools() {
        return availableTools;
    }
} 
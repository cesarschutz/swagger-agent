package com.example.swaggeragent.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import com.example.swaggeragent.dto.ChatResponse;
import com.example.swaggeragent.model.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço responsável por gerenciar as conversas de chat.
 * Suporta tanto chat síncrono quanto streaming assíncrono.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final OpenAiChatModel chatModel;
    private final OpenApiParserService openApiParserService;
    private final DynamicToolGeneratorService dynamicToolGeneratorService;
    private final SystemPromptService systemPromptService;
    private final ToolLoggerService toolLoggerService;

    private ChatClient chatClient;
    private List<DynamicTool> currentTools;
    private List<FunctionCallback> currentFunctionCallbacks;
    private final ConcurrentHashMap<String, InMemoryChatMemory> chatMemories = new ConcurrentHashMap<>();

    public ChatService(
            OpenAiChatModel chatModel,
            OpenApiParserService openApiParserService,
            DynamicToolGeneratorService dynamicToolGeneratorService,
            SystemPromptService systemPromptService,
            ToolLoggerService toolLoggerService) {
        this.chatModel = chatModel;
        this.openApiParserService = openApiParserService;
        this.dynamicToolGeneratorService = dynamicToolGeneratorService;
        this.systemPromptService = systemPromptService;
        this.toolLoggerService = toolLoggerService;
    }

    @PostConstruct
    public void initialize() {
        loadToolsAndInitializeChat();
    }

    /**
     * Carrega as ferramentas dos arquivos OpenAPI e inicializa o chat.
     */
    public void loadToolsAndInitializeChat() {
        try {
            // Faz o parsing dos arquivos OpenAPI
            List<OpenApiEndpoint> endpoints = openApiParserService.parseAllOpenApiFiles();

            // Gera ferramentas dinâmicas
            currentTools = dynamicToolGeneratorService.generateToolsFromEndpoints(endpoints);

            // Faz o log das ferramentas
            toolLoggerService.logTools(currentTools);

            // Converte para function callbacks
            currentFunctionCallbacks = dynamicToolGeneratorService.convertToFunctionCallbacks(currentTools);

            // Inicializa o cliente de chat com as ferramentas
            initializeChatClient();

            log.info("Carregadas com sucesso {} ferramentas das especificações OpenAPI", currentTools.size());

        } catch (Exception e) {
            log.error("Erro ao carregar ferramentas e inicializar chat", e);
        }
    }

    /**
     * Inicializa o cliente de chat com as ferramentas disponíveis.
     */
    private void initializeChatClient() {
        String systemPrompt = systemPromptService.generateSystemPrompt(currentTools);
        log.debug("Prompt do Sistema: {}", systemPrompt);

        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultSystem(systemPrompt);

        // Adiciona function callbacks se disponíveis
        if (currentFunctionCallbacks != null && !currentFunctionCallbacks.isEmpty()) {
            builder = builder.defaultFunctions(currentFunctionCallbacks.toArray(new FunctionCallback[0]));
        }

        chatClient = builder.build();

        log.debug("Cliente de chat inicializado com {} function callbacks",
                currentFunctionCallbacks != null ? currentFunctionCallbacks.size() : 0);
    }

    /**
     * Processa uma mensagem de chat de forma síncrona.
     * 
     * @param message Mensagem do usuário
     * @param sessionId ID da sessão
     * @return Resposta completa do chat
     */
    public ChatResponse chat(String message, String sessionId) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setRole("assistant");

        if (chatClient == null) {
            chatResponse.setContent("Erro: Chat não foi inicializado. Verifique os logs para mais detalhes.");
            return chatResponse;
        }

        try {
            // Obtém ou cria memória de chat para esta sessão
            InMemoryChatMemory chatMemory = chatMemories.computeIfAbsent(sessionId, k -> new InMemoryChatMemory());

            // Cria cliente de chat com memória para esta sessão
            ChatClient sessionChatClient = ChatClient.builder(chatModel)
                    .defaultSystem(systemPromptService.generateSystemPrompt(currentTools))
                    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                    .defaultFunctions(currentFunctionCallbacks != null ?
                            currentFunctionCallbacks.toArray(new FunctionCallback[0]) : new FunctionCallback[0])
                    .build();

            String response = sessionChatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            log.debug("Resposta de chat gerada para sessão: {}", sessionId);

            chatResponse.setContent(response);
            return chatResponse;

        } catch (Exception e) {
            log.error("Erro ao processar mensagem de chat para sessão: {}", sessionId, e);
            chatResponse.setContent("Erro ao processar mensagem: " + e.getMessage());
            return chatResponse;
        }
    }

    /**
     * Processa uma mensagem de chat com streaming assíncrono.
     * Retorna a resposta em chunks conforme é gerada.
     * 
     * @param message Mensagem do usuário
     * @param sessionId ID da sessão
     * @return Flux de chunks da resposta
     */
    public Flux<String> chatStream(String message, String sessionId) {
        if (chatClient == null) {
            return Flux.just("data: Erro: Chat não foi inicializado. Verifique os logs para mais detalhes.\n\n");
        }

        try {
            // Obtém ou cria memória de chat para esta sessão
            InMemoryChatMemory chatMemory = chatMemories.computeIfAbsent(sessionId, k -> new InMemoryChatMemory());

            // Cria cliente de chat com memória para esta sessão
            ChatClient sessionChatClient = ChatClient.builder(chatModel)
                    .defaultSystem(systemPromptService.generateSystemPrompt(currentTools))
                    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                    .defaultFunctions(currentFunctionCallbacks != null ?
                            currentFunctionCallbacks.toArray(new FunctionCallback[0]) : new FunctionCallback[0])
                    .build();

            log.debug("Iniciando chat streaming para sessão: {}", sessionId);

            // Retorna o stream da resposta formatado como Server-Sent Events
            return sessionChatClient.prompt()
                    .user(message)
                    .stream()
                    .content()
                    .map(chunk -> "data: " + chunk + "\n\n")
                    .concatWith(Flux.just("data: [DONE]\n\n"))
                    .delayElements(Duration.ofMillis(50)) // Pequeno delay para melhor UX
                    .doOnComplete(() -> log.debug("Chat streaming concluído para sessão: {}", sessionId))
                    .doOnError(error -> log.error("Erro no chat streaming para sessão: {}", sessionId, error))
                    .onErrorReturn("data: Erro ao processar mensagem em streaming\n\n");

        } catch (Exception e) {
            log.error("Erro ao iniciar chat streaming para sessão: {}", sessionId, e);
            return Flux.just("data: Erro ao processar mensagem: " + e.getMessage() + "\n\n");
        }
    }

    /**
     * Retorna as ferramentas atualmente carregadas.
     * 
     * @return Lista de ferramentas dinâmicas
     */
    public List<DynamicTool> getCurrentTools() {
        return currentTools;
    }

    /**
     * Retorna o número de ferramentas carregadas.
     * 
     * @return Número de ferramentas
     */
    public int getToolCount() {
        return currentTools != null ? currentTools.size() : 0;
    }

    /**
     * Limpa a memória de chat para uma sessão específica.
     * 
     * @param sessionId ID da sessão
     */
    public void clearChatMemory(String sessionId) {
        chatMemories.remove(sessionId);
        log.info("Memória de chat limpa para sessão: {}", sessionId);
    }

    /**
     * Limpa todas as memórias de chat.
     */
    public void clearAllChatMemories() {
        chatMemories.clear();
        log.info("Todas as memórias de chat foram limpas");
    }
} 
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

import com.example.swaggeragent.dto.ChatResponse;
import com.example.swaggeragent.model.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    public void loadToolsAndInitializeChat() {
        try {
            // Parse OpenAPI files
            List<OpenApiEndpoint> endpoints = openApiParserService.parseAllOpenApiFiles();

            // Generate dynamic tools
            currentTools = dynamicToolGeneratorService.generateToolsFromEndpoints(endpoints);

            // Log the tools
            toolLoggerService.logTools(currentTools);

            // Convert to function callbacks
            currentFunctionCallbacks = dynamicToolGeneratorService.convertToFunctionCallbacks(currentTools);

            // Initialize chat client with tools
            initializeChatClient();

            log.info("Successfully loaded {} tools from OpenAPI specifications", currentTools.size());

        } catch (Exception e) {
            log.error("Error loading tools and initializing chat", e);
        }
    }

    private void initializeChatClient() {
        String systemPrompt = systemPromptService.generateSystemPrompt(currentTools);
        log.debug("System Prompt: {}", systemPrompt);

        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultSystem(systemPrompt);

        // Add function callbacks if available
        if (currentFunctionCallbacks != null && !currentFunctionCallbacks.isEmpty()) {
            builder = builder.defaultFunctions(currentFunctionCallbacks.toArray(new FunctionCallback[0]));
        }

        chatClient = builder.build();

        log.debug("Chat client initialized with {} function callbacks",
                currentFunctionCallbacks != null ? currentFunctionCallbacks.size() : 0);
    }

    public ChatResponse chat(String message, String sessionId) {

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setRole("assistent");

        if (chatClient == null) {
            chatResponse.setContent("Erro: Chat não foi inicializado. Verifique os logs para mais detalhes.");
            return chatResponse;
        }

        try {
            // Get or create chat memory for this session
            InMemoryChatMemory chatMemory = chatMemories.computeIfAbsent(sessionId, k -> new InMemoryChatMemory());

            // Create chat client with memory for this session
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

            log.debug("Chat response generated for session: {}", sessionId);

            chatResponse.setContent(response);
            return chatResponse;

        } catch (Exception e) {
            log.error("Error processing chat message for session: {}", sessionId, e);
            chatResponse.setContent("Erro ao processar mensagem: " + e.getMessage());
            return chatResponse;
        }
    }

    public List<DynamicTool> getCurrentTools() {
        return currentTools;
    }

    public int getToolCount() {
        return currentTools != null ? currentTools.size() : 0;
    }

    public void clearChatMemory(String sessionId) {
        chatMemories.remove(sessionId);
        log.info("Chat memory cleared for session: {}", sessionId);
    }

    public void clearAllChatMemories() {
        chatMemories.clear();
        log.info("All chat memories cleared");
    }
} 
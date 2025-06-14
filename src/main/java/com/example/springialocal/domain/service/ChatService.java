package com.example.springialocal.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import com.example.springialocal.application.dto.ChatResponse;
import com.example.springialocal.domain.tool.model.DynamicTool;
import com.example.springialocal.domain.tool.model.OpenApiEndpoint;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final OpenAiChatModel chatModel;
    private final OpenApiParserService openApiParserService;
    private final DynamicToolGeneratorService dynamicToolGeneratorService;

    private ChatClient chatClient;
    private List<DynamicTool> currentTools;
    private List<FunctionCallback> currentFunctionCallbacks;
    private final ConcurrentHashMap<String, InMemoryChatMemory> chatMemories = new ConcurrentHashMap<>();


    public ChatService(
        OpenAiChatModel chatModel,
        OpenApiParserService openApiParserService,
        DynamicToolGeneratorService dynamicToolGeneratorService) {
            this.chatModel = chatModel;
            this.openApiParserService = openApiParserService;
            this.dynamicToolGeneratorService = dynamicToolGeneratorService;
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
        String systemPrompt = generateSystemPrompt();
        
        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultSystem(systemPrompt);
        
        // Add function callbacks if available
        if (currentFunctionCallbacks != null && !currentFunctionCallbacks.isEmpty()) {
            builder = builder.defaultFunctions(currentFunctionCallbacks.toArray(new FunctionCallback[0]));
        }
        
        chatClient = builder.build();
        
        log.debug("Chat client initialized with {} function callbacks", 
                 currentFunctionCallbacks != null ? currentFunctionCallbacks.size() : 0);
    }

    private String generateSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Você é um assistente AI especializado em executar operações através de APIs. ");
        prompt.append("Você tem acesso a várias ferramentas que correspondem a endpoints de diferentes microserviços. ");
        prompt.append("Sempre que o usuário solicitar uma operação, analise qual ferramenta é mais apropriada e execute-a. ");
        prompt.append("Seja preciso e forneça respostas claras sobre os resultados das operações.\n\n");
        
        if (currentTools != null && !currentTools.isEmpty()) {
            prompt.append("FERRAMENTAS DISPONÍVEIS:\n");
            for (DynamicTool tool : currentTools) {
                prompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
                prompt.append("  Endpoint: ").append(tool.getEndpoint().method().toUpperCase())
                      .append(" ").append(tool.getEndpoint().path()).append("\n");
                
                if (tool.getEndpoint().parameters() != null && !tool.getEndpoint().parameters().isEmpty()) {
                    prompt.append("  Parâmetros: ");
                    List<String> paramDescriptions = tool.getEndpoint().parameters().stream()
                            .map(p -> p.name() + " (" + p.type() + ")" + (p.required() ? " [OBRIGATÓRIO]" : " [OPCIONAL]"))
                            .toList();
                    prompt.append(String.join(", ", paramDescriptions));
                    prompt.append("\n");
                }
                prompt.append("\n");
            }
        } else {
            prompt.append("ATENÇÃO: Nenhuma ferramenta está disponível no momento. ");
            prompt.append("Verifique se os arquivos OpenAPI estão presentes na pasta openapi-specs.\n");
        }
        
        prompt.append("\nSempre explique qual ferramenta você está usando e por quê. ");
        prompt.append("Se uma operação falhar, explique o erro e sugira alternativas quando possível.\n\n");
        
        prompt.append("INSTRUÇÕES DE FORMATAÇÃO E COMPORTAMENTO:\n");
        prompt.append("- Sempre formate suas respostas usando Markdown para garantir clareza e elegância.\n");
        prompt.append("- Use emojis de forma apropriada para tornar a resposta mais amigável e fácil de ler (por exemplo, :white_check_mark: para sucesso, :x: para erro).\n");
        prompt.append("- Se o usuário perguntar sobre as ferramentas que você possui, liste o nome e a descrição de cada uma de forma organizada.\n\n");

        prompt.append("IMPORTANTE: O resultado da execução de uma ferramenta será sempre um objeto JSON com dois campos: 'httpStatusCode' (o código de status HTTP numérico da resposta) e 'body' (o corpo da resposta como uma string, que pode ser um JSON ou texto simples). ");
        prompt.append("Use o 'httpStatusCode' para determinar o resultado da operação: códigos 2xx indicam sucesso, 4xx indicam um erro do cliente (ex: dados inválidos) e 5xx indicam um erro no servidor. ");
        prompt.append("Baseie sua resposta final ao usuário no 'httpStatusCode' e no 'body' recebido, explicando o resultado de forma clara.");
        
        System.out.println("---> prompt: " + prompt.toString());
        return prompt.toString();
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
                    .defaultSystem(generateSystemPrompt())
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



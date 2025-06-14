package com.example.springialocal.domain.service;

import com.example.springialocal.application.dto.ChatResponse;
import com.example.springialocal.domain.tool.CardAccountApiTools;
import com.example.springialocal.domain.tool.InvoiceApiTools;

import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatClient chat;

    public ChatService(OpenAiChatModel model, CardAccountApiTools cardAccountApiTools, InvoiceApiTools invoiceApiTools) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Você é um assistente AI especializado em executar operações através de APIs. ");
        prompt.append("Você tem acesso a várias ferramentas que correspondem a endpoints de diferentes microserviços. ");
        prompt.append("Sempre que o usuário solicitar uma operação, analise qual ferramenta é mais apropriada e execute-a. ");
        prompt.append("Seja preciso e forneça respostas claras sobre os resultados das operações.\n\n");

        this.chat = ChatClient.builder(model)
            .defaultSystem(prompt.toString())
            .defaultTools(cardAccountApiTools, invoiceApiTools)
            .build();
    }

    public ChatResponse generateResponse(String prompt) {
        try {
            ChatResponse message = new ChatResponse();
            message.setRole("assistant");
            message.setContent(chat.prompt(prompt).call().content());
            return message;
        } catch (Exception e) {
            logger.error("Erro ao gerar resposta do modelo IA", e);
            return new ChatResponse("assistant", "Desculpe, ocorreu um erro ao processar sua mensagem.");
        }
    }
}
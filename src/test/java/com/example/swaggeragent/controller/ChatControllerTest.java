package com.example.swaggeragent.controller;

import com.example.swaggeragent.dto.ChatRequest;
import com.example.swaggeragent.dto.ChatResponse;
import com.example.swaggeragent.service.chat.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o {@link ChatController}.
 * <p>
 * Esta classe de teste utiliza {@link WebMvcTest} para testar o controller
 * de chat de forma isolada, mockando apenas o {@link ChatService} e testando
 * a integração entre a camada web e a camada de serviço.
 */
@WebMvcTest(controllers = ChatController.class)
public class ChatControllerTest {

    /**
     * MockMvc para simular requisições HTTP e verificar respostas.
     * <p>
     * Permite testar endpoints sem iniciar um servidor completo,
     * simulando requisições HTTP e verificando respostas de forma
     * programática.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * ObjectMapper para serialização/deserialização de JSON nos testes.
     * <p>
     * Utilizado para converter objetos Java em JSON para enviar nas
     * requisições de teste e verificar a estrutura das respostas.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Mock do ChatService para isolar o controller durante os testes.
     * <p>
     * Permite controlar o comportamento do serviço e verificar se
     * os métodos corretos são chamados com os parâmetros esperados.
     */
    @MockBean
    private ChatService chatService;

    /**
     * Testa o endpoint de chat com uma requisição válida.
     *
     * @throws Exception se houver erro durante a execução do teste
     */
    @Test
    void chat_whenValidRequest_shouldReturnSuccess() throws Exception {
        // Given - Prepara os dados de teste
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        request.setSessionId("test-session");

        // Configura o mock do ChatService para retornar uma resposta específica
        ChatResponse mockResponse = new ChatResponse("assistant", "Hi there!");
        when(chatService.processChatMessage(anyString(), anyString())).thenReturn(mockResponse);

        // When & Then - Executa a requisição e verifica os resultados
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.role").value("assistant"))
                .andExpect(jsonPath("$.content").value("Hi there!"));
    }

    /**
     * Testa o endpoint de chat com uma requisição inválida.
     *
     * @throws Exception se houver erro durante a execução do teste
     */
    @Test
    void chat_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        // Given - Prepara uma requisição inválida (sem mensagem)
        ChatRequest request = new ChatRequest(); // message é null
        request.setSessionId("test-session-bad");

        // When & Then - Executa a requisição e verifica o erro
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 
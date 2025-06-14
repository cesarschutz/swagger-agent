package com.example.springialocal.application.controller;

import com.example.springialocal.application.dto.ChatRequest;
import com.example.springialocal.application.dto.ChatResponse;
import com.example.springialocal.domain.service.ChatService;
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

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void chat_whenValidRequest_shouldReturnSuccess() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        ChatResponse mockResponse = new ChatResponse("assistant", "Hi there!");
        when(chatService.generateResponse(anyString())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.role").value("assistant"))
                .andExpect(jsonPath("$.content").value("Hi there!"));
    }

    @Test
    void chat_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        // Given
        ChatRequest request = new ChatRequest(); // message is null

        // When & Then
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 
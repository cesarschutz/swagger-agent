package com.example.springialocal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.swaggeragent.model.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiParameter;
import com.example.swaggeragent.service.SystemPromptService;
import com.example.swaggeragent.service.ToolLoggerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemPromptServiceTest {

    private SystemPromptService systemPromptService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        ToolLoggerService toolLoggerService = new ToolLoggerService(objectMapper);
        systemPromptService = new SystemPromptService(toolLoggerService);
    }

    @Test
    void generateSystemPrompt_withTools_shouldContainToolInfo() {
        // Given
        List<OpenApiParameter> parameters = List.of(
                new OpenApiParameter("param1", "query", "description1", true, "string", null, null, null, null),
                new OpenApiParameter("param2", "header", "description2", false, "integer", null, null, null, null)
        );
        var endpoint = new OpenApiEndpoint("test-endpoint", "GET", "/test", "Test Summary", "Test Description", "http://localhost:8080", parameters, null, null, null);
        var tool = DynamicTool.builder().name("testTool").description("A test tool").endpoint(endpoint).build();
        List<DynamicTool> tools = Collections.singletonList(tool);

        // When
        String prompt = systemPromptService.generateSystemPrompt(tools);

        // Then
        assertTrue(prompt.contains("FERRAMENTAS DISPONÍVEIS:"));
        assertTrue(prompt.contains("testTool"));
        assertTrue(prompt.contains("A test tool"));
        assertTrue(prompt.contains("GET /test"));
        assertTrue(prompt.contains("param1 (string) [OBRIGATÓRIO]"));
    }

    @Test
    void generateSystemPrompt_withoutTools_shouldContainWarning() {
        // Given
        List<DynamicTool> tools = Collections.emptyList();

        // When
        String prompt = systemPromptService.generateSystemPrompt(tools);

        // Then
        assertTrue(prompt.contains("ATENÇÃO: Nenhuma ferramenta está disponível no momento."));
        assertFalse(prompt.contains("FERRAMENTAS DISPONÍVEIS:"));
    }
} 
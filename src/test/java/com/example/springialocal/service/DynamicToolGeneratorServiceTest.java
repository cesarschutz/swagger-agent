package com.example.springialocal.service;

import com.example.swaggeragent.model.*;
import com.example.swaggeragent.service.DynamicToolGeneratorService;
import com.example.swaggeragent.service.OpenApiParserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicToolGeneratorServiceTest {

    @Mock
    private OpenApiParserService openApiParserService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DynamicToolGeneratorService dynamicToolGeneratorService;

    @BeforeEach
    void setUp() {
        // This simulates a successful response from the RestTemplate
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"result\":\"success\"}", HttpStatus.OK));
    }

    @Test
    void generateTools_shouldCreateDynamicTools() {
        // Given
        List<OpenApiParameter> parameters = List.of(
                new OpenApiParameter("param1", "query", "description1", true, "string", null, null, null, null)
        );
        var requestBody = new OpenApiRequestBody("Request body description", true, Map.of("application/json", new OpenApiMediaType(new Schema<>(), "{}")));
        var response = new OpenApiResponse("Success", Map.of("application/json", new OpenApiMediaType(new Schema<>(), "{}")));

        List<OpenApiEndpoint> endpoints = List.of(
                new OpenApiEndpoint(
                        "test-endpoint",
                        "GET",
                        "/test",
                        "Test Summary",
                        "Test Description",
                        "http://localhost:8080",
                        parameters,
                        requestBody,
                        Map.of("200", response),
                        List.of("test")
                )
        );

        when(openApiParserService.parseAllOpenApiFiles()).thenReturn(endpoints);

        // When
        List<DynamicTool> tools = dynamicToolGeneratorService.generateToolsFromEndpoints(endpoints);
        assertFalse(tools.isEmpty());
        DynamicTool tool = tools.get(0);
        Function<Object, String> function = tool.getFunction();

        // This simulates the input from the Spring AI framework (a deserialized JSON object)
        Map<String, Object> functionInput = Map.of("param1", "value1");

        // Then
        String result = function.apply(functionInput);

        assertNotNull(result);
        assertEquals("{\"httpStatusCode\":200,\"body\":\"{\\\"result\\\":\\\"success\\\"}\"}", result);
    }
} 
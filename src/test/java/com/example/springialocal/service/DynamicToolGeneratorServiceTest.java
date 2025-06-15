package com.example.springialocal.service;

import com.example.springialocal.model.DynamicTool;
import com.example.springialocal.model.OpenApiEndpoint;
import com.example.springialocal.model.OpenApiParameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicToolGeneratorServiceTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.Builder webClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    private DynamicToolGeneratorService dynamicToolGeneratorService;

    @BeforeEach
    void setUp() {
        dynamicToolGeneratorService = new DynamicToolGeneratorService(objectMapper, webClientBuilder, false);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void generatedFunction_shouldAcceptMapAndReturnString() throws Exception {
        // Given
        var parameter = new OpenApiParameter("param1", "query", "Test parameter", true, "string", null, null, null);
        var endpoint = new OpenApiEndpoint(
                "test-endpoint",
                "GET",
                "/test",
                "Test Summary",
                "Test Description",
                "http://localhost:8080",
                List.of(parameter),
                null,
                null,
                null
        );

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = webClient.get();
        WebClient.RequestHeadersSpec requestHeadersSpec = requestHeadersUriSpec.uri(any(String.class));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchange()).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"result\":\"success\"}")
                .build()));

        // When
        List<DynamicTool> tools = dynamicToolGeneratorService.generateToolsFromEndpoints(Collections.singletonList(endpoint));
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
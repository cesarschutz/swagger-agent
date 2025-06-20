package com.example.swaggeragent.model.response;

/**
 * Representa o resultado da execução de uma ferramenta (tool).
 * <p>
 * Este record armazena o resultado de uma chamada de API realizada por uma {@link DynamicTool},
 * encapsulando o código de status HTTP e o corpo (body) da resposta.
 *
 * @param httpStatusCode o código de status HTTP retornado pela chamada à API.
 * @param body           o corpo da resposta (response body) da chamada à API como uma String.
 */
public record ToolExecutionResult(int httpStatusCode, String body) {
} 
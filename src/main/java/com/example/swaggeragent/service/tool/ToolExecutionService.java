package com.example.swaggeragent.service.tool;

import com.example.swaggeragent.model.response.ToolExecutionResult;
import com.example.swaggeragent.model.OpenApiEndpoint;

/**
 * Interface para execução de ferramentas (tools) dinâmicas.
 * <p>
 * Esta interface define o contrato para execução de ferramentas que foram
 * geradas dinamicamente a partir de especificações OpenAPI. A implementação
 * principal é o {@link ApiExecutionService}, que utiliza o WebClient para
 * fazer chamadas HTTP reativas a APIs externas.
 * <p>
 * <b>Responsabilidades:</b>
 * <ul>
 *   <li>Executar chamadas HTTP para endpoints de APIs externas</li>
 *   <li>Processar parâmetros de entrada em formato JSON</li>
 *   <li>Retornar resultados padronizados com status HTTP e corpo da resposta</li>
 *   <li>Tratar erros de execução de forma consistente</li>
 * </ul>
 * <p>
 * <b>Fluxo de execução:</b>
 * <ol>
 *   <li>Recebe um endpoint OpenAPI e parâmetros JSON</li>
 *   <li>Constrói a URL completa com parâmetros de path e query</li>
 *   <li>Monta os cabeçalhos HTTP (incluindo autenticação)</li>
 *   <li>Executa a chamada HTTP usando WebClient</li>
 *   <li>Retorna o resultado encapsulado em {@link ToolExecutionResult}</li>
 * </ol>
 * <p>
 * <b>Implementações:</b>
 * <ul>
 *   <li>{@link ApiExecutionService} - Implementação principal com WebClient</li>
 * </ul>
 */
public interface ToolExecutionService {
    
    /**
     * Executa uma ferramenta com base em um endpoint e parâmetros.
     * <p>
     * Este método é responsável por:
     * <ul>
     *   <li>Interpretar a definição do endpoint OpenAPI</li>
     *   <li>Processar os parâmetros JSON de entrada</li>
     *   <li>Construir a requisição HTTP apropriada</li>
     *   <li>Executar a chamada à API externa</li>
     *   <li>Retornar o resultado padronizado</li>
     * </ul>
     * <p>
     * <b>Tratamento de erros:</b> A implementação deve capturar e tratar
     * erros de rede, timeouts, e respostas de erro das APIs, retornando
     * sempre um {@link ToolExecutionResult} válido.
     *
     * @param endpoint o endpoint da API definido na especificação OpenAPI
     * @param input    os parâmetros de entrada em formato JSON string
     * @return o resultado da execução contendo status HTTP e corpo da resposta
     */
    ToolExecutionResult execute(OpenApiEndpoint endpoint, String input);
} 
package com.example.swaggeragent.service.tool;

import com.example.swaggeragent.model.response.ToolExecutionResult;
import com.example.swaggeragent.model.OpenApiEndpoint;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Serviço para execução assíncrona de ferramentas dinâmicas.
 * <p>
 * Esta classe fornece um wrapper assíncrono para a execução de ferramentas,
 * permitindo que chamadas a APIs externas sejam executadas em threads separadas
 * sem bloquear a thread principal da aplicação.
 * <p>
 * <b>Características:</b>
 * <ul>
 *   <li><b>Execução não-bloqueante:</b> Usa {@link CompletableFuture} para operações assíncronas</li>
 *   <li><b>Thread pool dedicado:</b> Utiliza o executor configurado com {@code @EnableAsync}</li>
 *   <li><b>Delegação:</b> Delega a execução real para o {@link ToolExecutionService}</li>
 *   <li><b>Compatibilidade:</b> Mantém a mesma interface de resultado</li>
 * </ul>
 * <p>
 * <b>Uso:</b> Este serviço é útil quando se deseja executar múltiplas
 * ferramentas em paralelo ou quando a execução pode levar tempo considerável.
 * <p>
 * <b>Exemplo:</b>
 * <pre>
 * CompletableFuture&lt;ToolExecutionResult&gt; future = 
 *     asyncToolExecutionService.executeAsync(endpoint, inputJson);
 * ToolExecutionResult result = future.get(); // aguarda o resultado
 * </pre>
 */
@Service
public class AsyncToolExecutionService {
    
    private final ToolExecutionService toolExecutionService;

    /**
     * Constrói uma nova instância do serviço assíncrono.
     *
     * @param toolExecutionService o serviço de execução de ferramentas a ser usado
     */
    public AsyncToolExecutionService(ToolExecutionService toolExecutionService) {
        this.toolExecutionService = toolExecutionService;
    }

    /**
     * Executa uma ferramenta de forma assíncrona.
     * <p>
     * Este método executa a ferramenta em uma thread separada usando o executor
     * configurado com a anotação {@code @EnableAsync}. O resultado é retornado
     * como um {@link CompletableFuture} que pode ser aguardado ou processado
     * de forma não-bloqueante.
     * <p>
     * <b>Vantagens:</b>
     * <ul>
     *   <li>Não bloqueia a thread principal da aplicação</li>
     *   <li>Permite execução paralela de múltiplas ferramentas</li>
     *   <li>Facilita o controle de timeout e cancelamento</li>
     * </ul>
     * <p>
     * <b>Tratamento de erros:</b> Exceções lançadas durante a execução
     * são capturadas e podem ser tratadas através do {@link CompletableFuture}.
     *
     * @param endpoint o endpoint da API a ser executado
     * @param input    os parâmetros de entrada em formato JSON
     * @return um {@link CompletableFuture} que será completado com o resultado da execução
     */
    @Async
    public CompletableFuture<ToolExecutionResult> executeAsync(OpenApiEndpoint endpoint, String input) {
        // Executa a ferramenta de forma síncrona na thread assíncrona
        ToolExecutionResult result = toolExecutionService.execute(endpoint, input);
        
        // Retorna o resultado como um CompletableFuture já completado
        return CompletableFuture.completedFuture(result);
    }
} 
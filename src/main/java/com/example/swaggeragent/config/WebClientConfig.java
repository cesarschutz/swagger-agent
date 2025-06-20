package com.example.swaggeragent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;

/**
 * Configuração do cliente WebFlux para chamadas HTTP reativas.
 * <p>
 * Esta classe é responsável por configurar e fornecer um bean do {@link WebClient}
 * otimizado para ser utilizado em toda a aplicação para chamadas HTTP reativas.
 * <p>
 * <b>Características da configuração:</b>
 * <ul>
 *   <li><b>Connection Pooling:</b> Pool de conexões com máximo de 100 conexões simultâneas</li>
 *   <li><b>Timeout de Aquisição:</b> 30 segundos para aguardar conexão disponível</li>
 *   <li><b>Timeout de Resposta:</b> 10 segundos para receber resposta completa</li>
 *   <li><b>Reatividade:</b> Baseado em Project Reactor para operações não-bloqueantes</li>
 * </ul>
 */
@Configuration
public class WebClientConfig {

    /**
     * Cria e configura um bean do {@link WebClient} com connection pooling otimizado.
     * <p>
     * O WebClient configurado possui as seguintes otimizações:
     * <ul>
     *   <li><b>ConnectionProvider personalizado:</b> "swagger-agent-pool" com configurações específicas</li>
     *   <li><b>Máximo de conexões:</b> 100 conexões simultâneas para alta concorrência</li>
     *   <li><b>Timeout de aquisição:</b> 30 segundos para aguardar conexão disponível no pool</li>
     *   <li><b>Timeout de resposta:</b> 10 segundos para receber resposta completa da API</li>
     * </ul>
     *
     * @return uma instância de {@link WebClient} configurada para uso em toda a aplicação
     */
    @Bean
    public WebClient webClient() {
        // Configura o pool de conexões com limites e timeouts
        ConnectionProvider provider = ConnectionProvider.builder("swagger-agent-pool")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();
        
        // Cria o cliente HTTP com o pool configurado e timeout de resposta
        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(Duration.ofSeconds(10));
        
        // Constrói o WebClient com o conector HTTP personalizado
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
} 
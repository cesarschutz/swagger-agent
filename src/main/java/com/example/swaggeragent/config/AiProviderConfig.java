package com.example.swaggeragent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Configuração condicional para provedores de IA.
 * <p>
 * Esta classe configura qual provedor de IA será utilizado baseado na propriedade
 * {@code app.ai.provider}. Apenas um provedor será carregado por vez:
 * <ul>
 *   <li><b>openai:</b> Utiliza o OpenAiChatModel (padrão)</li>
 *   <li><b>ollama:</b> Utiliza o OllamaChatModel</li>
 * </ul>
 * <p>
 * <b>Uso:</b> Configure a propriedade {@code app.ai.provider} no arquivo
 * {@code application.yml} ou via variável de ambiente {@code AI_PROVIDER}.
 */
@Configuration
public class AiProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(AiProviderConfig.class);

    @Autowired
    private SwaggerAgentProperties properties;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:deepseek-r1:7b}")
    private String ollamaModel;

    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private Double ollamaTemperature;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double openaiTemperature;

    /**
     * Configura o modelo OpenAI como provedor de IA.
     * <p>
     * Esta configuração é ativada quando {@code app.ai.provider=openai} ou
     * quando a propriedade não está definida (valor padrão).
     * <p>
     * <b>Requisitos:</b>
     * <ul>
     *   <li>Variável de ambiente {@code OPENAI_API_KEY} configurada</li>
     *   <li>Dependência {@code spring-ai-openai-spring-boot-starter} no classpath</li>
     * </ul>
     *
     * @param openAiChatModel o modelo OpenAI injetado pelo Spring AI
     * @return o modelo OpenAI configurado como provedor primário
     */
    @Bean("primaryChatModel")
    @Primary
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai", matchIfMissing = true)
    public ChatModel openAiChatModel(OpenAiChatModel openAiChatModel) {
        log.info("🔧 Configurando OpenAI como provedor de IA");
        log.info("🔧 Provedor configurado: {}", properties.getAi().getProvider());
        log.info("🔧 Modelo OpenAI configurado: {} com temperatura: {}", openaiModel, openaiTemperature);
        return openAiChatModel;
    }

    /**
     * Configura o modelo Ollama como provedor de IA.
     * <p>
     * Esta configuração é ativada quando {@code app.ai.provider=ollama}.
     * <p>
     * <b>Requisitos:</b>
     * <ul>
     *   <li>Servidor Ollama rodando (configurado via {@code spring.ai.ollama.base-url})</li>
     *   <li>Modelo baixado no Ollama (configurado via {@code spring.ai.ollama.chat.options.model})</li>
     *   <li>Dependência {@code spring-ai-ollama-spring-boot-starter} no classpath</li>
     * </ul>
     *
     * @param ollamaChatModel o modelo Ollama injetado pelo Spring AI
     * @return o modelo Ollama configurado como provedor primário
     */
    @Bean("primaryChatModel")
    @Primary
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
    public ChatModel ollamaChatModel(OllamaChatModel ollamaChatModel) {
        log.info("🔧 Configurando Ollama como provedor de IA");
        log.info("🔧 Provedor configurado: {}", properties.getAi().getProvider());
        log.info("🔧 URL base do Ollama: {}", ollamaBaseUrl);
        log.info("🔧 Modelo Ollama configurado: {} com temperatura: {}", ollamaModel, ollamaTemperature);
        return ollamaChatModel;
    }
} 
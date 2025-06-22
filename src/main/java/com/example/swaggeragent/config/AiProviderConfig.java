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

    @Value("${spring.ai.ollama.chat.options.model:llama3.1:8b}")
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
        log.info("\n" +
                "╔══════════════════════════════════════════════════════════════════════════════╗\n" +
                "║                       🤖 CONFIGURAÇÃO OPENAI                                 ║\n" +
                "╚══════════════════════════════════════════════════════════════════════════════╝");
        log.info(String.format("║ %-72s ║", "🔧 Provedor: " + properties.getAi().getProvider()));
        log.info(String.format("║ %-72s ║", "🧠 Modelo: " + openaiModel));
        log.info(String.format("║ %-72s ║", "🌡️  Temperatura: " + openaiTemperature));
        log.info(String.format("║ %-72s ║", "🔑 API Key: " + (System.getenv("OPENAI_API_KEY") != null ? "✅ Encontrada" : "❌ NÃO ENCONTRADA")));
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");
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
        log.info("\n" +
                "╔══════════════════════════════════════════════════════════════════════════════╗\n" +
                "║                       🤖 CONFIGURAÇÃO OLLAMA                                 ║\n" +
                "╚══════════════════════════════════════════════════════════════════════════════╝");
        log.info(String.format("║ %-72s ║", "🔧 Provedor: " + properties.getAi().getProvider()));
        log.info(String.format("║ %-72s ║", "🌐 URL Base: " + ollamaBaseUrl));
        log.info(String.format("║ %-72s ║", "🧠 Modelo: " + ollamaModel));
        log.info(String.format("║ %-72s ║", "🌡️  Temperatura: " + ollamaTemperature));
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");
        return ollamaChatModel;
    }
} 
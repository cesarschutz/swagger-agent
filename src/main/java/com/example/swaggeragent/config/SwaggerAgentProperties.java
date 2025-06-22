package com.example.swaggeragent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Propriedades de configuração do Swagger Agent.
 * <p>
 * Esta classe centraliza todas as configurações customizáveis da aplicação,
 * permitindo ajustes via arquivo de propriedades ou variáveis de ambiente.
 * <p>
 * <b>Uso:</b> As propriedades podem ser configuradas no arquivo <code>application.yml</code>
 * usando o prefixo <code>app</code>.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class SwaggerAgentProperties {
    
    /**
     * Comprimento máximo do nome de uma ferramenta.
     * <p>
     * Limita o tamanho do nome das ferramentas geradas dinamicamente
     * para evitar nomes excessivamente longos.
     * <p>
     * <b>Valor padrão:</b> 64 caracteres
     */
    private int maxToolNameLength = 64;
    
    /**
     * Timeout para operações de chat.
     * <p>
     * Define o tempo máximo que uma operação de chat pode levar antes
     * de ser considerada como timeout.
     * <p>
     * <b>Valor padrão:</b> 30 segundos
     */
    private Duration chatTimeout = Duration.ofSeconds(30);
    
    /**
     * Tamanho máximo da memória de sessão.
     * <p>
     * Limita o número de mensagens que podem ser armazenadas na memória
     * de uma sessão de chat.
     * <p>
     * <b>Valor padrão:</b> 1000 mensagens
     */
    private int maxSessionMemorySize = 1000;
    
    /**
     * Diretório de especificações OpenAPI.
     * <p>
     * Define o caminho do diretório onde estão localizadas as especificações
     * OpenAPI (arquivos .yaml ou .json) que serão processadas para gerar
     * as ferramentas dinâmicas.
     * <p>
     * <b>Valor padrão:</b> "openapi-specs"
     */
    private String openApiDirectory = "openapi-specs";
    
    /**
     * URL de fallback para APIs sem servidor definido.
     * <p>
     * Quando uma especificação OpenAPI não define explicitamente um servidor,
     * esta URL será usada como fallback para construir as URLs completas
     * das APIs.
     * <p>
     * <b>Valor padrão:</b> "http://localhost:8080"
     */
    private String defaultFallbackUrl = "http://localhost:8080";

    private Ai ai = new Ai();
    private Tool tool = new Tool();
    private Security security = new Security();

    public static class Ai {
        private String provider;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    public static class Tool {
        // Classe mantida para futuras propriedades
    }

    public static class Security {
        // Classe mantida para futuras propriedades
    }
    
    // Getters e Setters
    
    public int getMaxToolNameLength() {
        return maxToolNameLength;
    }
    
    public void setMaxToolNameLength(int maxToolNameLength) {
        this.maxToolNameLength = maxToolNameLength;
    }
    
    public Duration getChatTimeout() {
        return chatTimeout;
    }
    
    public void setChatTimeout(Duration chatTimeout) {
        this.chatTimeout = chatTimeout;
    }
    
    public int getMaxSessionMemorySize() {
        return maxSessionMemorySize;
    }
    
    public void setMaxSessionMemorySize(int maxSessionMemorySize) {
        this.maxSessionMemorySize = maxSessionMemorySize;
    }
    
    public String getOpenApiDirectory() {
        return openApiDirectory;
    }
    
    public void setOpenApiDirectory(String openApiDirectory) {
        this.openApiDirectory = openApiDirectory;
    }
    
    public String getDefaultFallbackUrl() {
        return defaultFallbackUrl;
    }
    
    public void setDefaultFallbackUrl(String defaultFallbackUrl) {
        this.defaultFallbackUrl = defaultFallbackUrl;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }
} 
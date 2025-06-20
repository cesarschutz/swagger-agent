package com.example.swaggeragent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Serviço responsável por gerar o prompt de sistema (system prompt) para o modelo de linguagem.
 * <p>
 * O prompt de sistema é uma instrução fundamental que define o papel, o comportamento,
 * as regras e as capacidades do agente de IA. Este serviço centraliza a lógica de
 * criação desse prompt, garantindo que o agente se comporte de maneira consistente e segura.
 */
@Service
public class SystemPromptService {

    private static final Logger log = LoggerFactory.getLogger(SystemPromptService.class);
    
    private static final String PROMPT_FILE_PATH = "prompt.md";

    /**
     * Gera e retorna o prompt de sistema completo.
     * <p>
     * Este método lê o prompt de sistema do arquivo prompt.md localizado em src/main/resources.
     * Se houver algum problema na leitura do arquivo, retorna um prompt de fallback básico.
     *
     * @return uma string contendo o prompt de sistema a ser usado pelo {@link org.springframework.ai.chat.client.ChatClient}.
     */
    public String generateSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPT_FILE_PATH);
            String prompt = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("Prompt do sistema carregado com sucesso do arquivo: {}", PROMPT_FILE_PATH);
            return prompt;
        } catch (IOException e) {
            log.error("Erro ao carregar o prompt do sistema do arquivo: {}. Usando prompt de fallback.", PROMPT_FILE_PATH, e);
            return getFallbackPrompt();
        }
    }

    /**
     * Prompt de fallback caso o arquivo prompt.md não possa ser carregado.
     * <p>
     * Este prompt básico garante que o sistema continue funcionando mesmo se houver
     * problemas com o arquivo de configuração principal.
     *
     * @return um prompt básico de sistema
     */
    private String getFallbackPrompt() {
        return """
                Você é um assistente de IA especializado em integração com APIs RESTful.
                
                🚨 **REGRAS DE SEGURANÇA**:
                - `Authorization` e `Traffic-Code` são injetados automaticamente
                - **NUNCA** inclua esses campos no JSON de entrada
                - **TODAS** as operações que não sejam GET requerem confirmação explícita
                
                📋 **COMPORTAMENTO**:
                - Baseie respostas **EXCLUSIVAMENTE** nos dados das APIs
                - Nunca invente ou adivinhe valores
                - Use Markdown e emojis para formatação
                - Confirme antes de executar operações modificadoras
                
                ⚙️ **INTERPRETAÇÃO DE RESULTADOS**:
                - 2xx: ✅ Sucesso
                - 4xx: ⚠️ Erro do cliente
                - 5xx: ❌ Erro do servidor
                
                🛠️ **FERRAMENTAS UTILIZADAS**:
                Ao final de cada resposta que utilize ferramentas, adicione:
                ---
                🛠️ *Ferramentas Utilizadas: ```nome_da_ferramenta_1```, ```nome_da_ferramenta_2```*
                """;
    }
} 
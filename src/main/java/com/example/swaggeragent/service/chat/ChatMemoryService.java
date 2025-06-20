package com.example.swaggeragent.service.chat;

import org.springframework.ai.chat.memory.InMemoryChatMemory;

/**
 * Interface para gerenciamento de memória de chat por sessão.
 * <p>
 * Esta interface define o contrato para gerenciar a memória de conversas
 * de chat, permitindo que o agente de IA mantenha contexto entre mensagens
 * de uma mesma sessão de usuário.
 * <p>
 * <b>Responsabilidades:</b>
 * <ul>
 *   <li><b>Persistência de contexto:</b> Manter histórico de mensagens por sessão</li>
 *   <li><b>Isolamento de sessões:</b> Cada sessão tem sua própria memória independente</li>
 *   <li><b>Gerenciamento de ciclo de vida:</b> Criar, limpar e gerenciar memórias de sessão</li>
 *   <li><b>Integração com Spring AI:</b> Utilizar {@link InMemoryChatMemory} para compatibilidade</li>
 * </ul>
 * <p>
 * <b>Fluxo de uso:</b>
 * <ol>
 *   <li>Usuário inicia uma sessão com um ID único</li>
 *   <li>Serviço obtém ou cria memória para a sessão</li>
 *   <li>Mensagens são adicionadas à memória durante a conversa</li>
 *   <li>IA utiliza o contexto para gerar respostas mais relevantes</li>
 *   <li>Memória pode ser limpa quando necessário</li>
 * </ol>
 * <p>
 * <b>Implementações:</b>
 * <ul>
 *   <li>{@link ChatMemoryServiceImpl} - Implementação principal com armazenamento em memória</li>
 * </ul>
 * <p>
 * <b>Considerações de performance:</b> A implementação atual usa armazenamento
 * em memória, o que é adequado para desenvolvimento e testes. Para produção,
 * considere implementações com persistência em banco de dados.
 */
public interface ChatMemoryService {
    
    /**
     * Obtém ou cria uma memória de chat para uma sessão específica.
     * <p>
     * Este método implementa o padrão "get or create", onde:
     * <ul>
     *   <li>Se a sessão já existe, retorna a memória existente</li>
     *   <li>Se a sessão não existe, cria uma nova memória vazia</li>
     * </ul>
     * <p>
     * <b>Thread safety:</b> A implementação deve ser thread-safe para suportar
     * múltiplas sessões simultâneas.
     * <p>
     * <b>Performance:</b> A criação de nova memória deve ser eficiente,
     * pois pode ocorrer frequentemente com novos usuários.
     *
     * @param sessionId o ID único da sessão de chat
     * @return a memória de chat da sessão (nova ou existente)
     * @throws IllegalArgumentException se o sessionId for null ou vazio
     */
    InMemoryChatMemory getOrCreate(String sessionId);
    
    /**
     * Limpa a memória de chat de uma sessão específica.
     * <p>
     * Remove completamente a memória de uma sessão, liberando recursos
     * e permitindo que a sessão comece do zero na próxima interação.
     * <p>
     * <b>Uso típico:</b>
     * <ul>
     *   <li>Usuário solicita "limpar conversa"</li>
     *   <li>Timeout de sessão (implementação futura)</li>
     *   <li>Problemas de memória que requerem limpeza</li>
     * </ul>
     * <p>
     * <b>Idempotência:</b> Chamar este método múltiplas vezes para a mesma
     * sessão não deve causar erros.
     *
     * @param sessionId o ID da sessão cuja memória deve ser limpa
     */
    void clear(String sessionId);
    
    /**
     * Limpa todas as memórias de chat de todas as sessões.
     * <p>
     * Remove completamente todas as memórias de chat, liberando todos
     * os recursos associados. Esta operação é útil para:
     * <ul>
     *   <li>Manutenção do sistema</li>
     *   <li>Limpeza periódica de memória</li>
     *   <li>Reset completo do sistema</li>
     * </ul>
     * <p>
     * <b>Atenção:</b> Esta operação é destrutiva e remove o contexto
     * de todas as conversas ativas. Use com cuidado em produção.
     * <p>
     * <b>Performance:</b> Pode ser uma operação custosa dependendo do
     * número de sessões ativas.
     */
    void clearAll();
} 
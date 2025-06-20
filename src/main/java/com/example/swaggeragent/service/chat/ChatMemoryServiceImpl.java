package com.example.swaggeragent.service.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação do serviço de gerenciamento de memória de chat.
 * <p>
 * Esta classe implementa a interface {@link ChatMemoryService} utilizando
 * armazenamento em memória com um {@link ConcurrentHashMap} para garantir
 * thread-safety e performance em ambientes concorrentes.
 * <p>
 * <b>Características da implementação:</b>
 * <ul>
 *   <li><b>Thread-safe:</b> Usa {@link ConcurrentHashMap} para operações seguras em múltiplas threads</li>
 *   <li><b>Armazenamento em memória:</b> Todas as memórias são mantidas na JVM</li>
 *   <li><b>Integração Spring AI:</b> Utiliza {@link InMemoryChatMemory} para compatibilidade</li>
 *   <li><b>Logging detalhado:</b> Registra operações importantes para debugging</li>
 *   <li><b>Performance otimizada:</b> Operações O(1) para acesso e criação</li>
 * </ul>
 * <p>
 * <b>Estrutura de dados:</b>
 * <ul>
 *   <li><b>Chave:</b> String (sessionId)</li>
 *   <li><b>Valor:</b> {@link InMemoryChatMemory} (memória da sessão)</li>
 * </ul>
 * <p>
 * <b>Considerações de memória:</b> Como todas as memórias são mantidas em RAM,
 * o uso de memória cresce linearmente com o número de sessões ativas. Para
 * aplicações com muitas sessões simultâneas, considere implementar:
 * <ul>
 *   <li>Limpeza automática de sessões inativas</li>
 *   <li>Limite máximo de sessões</li>
 *   <li>Persistência em banco de dados</li>
 * </ul>
 */
@Service
public class ChatMemoryServiceImpl implements ChatMemoryService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatMemoryServiceImpl.class);
    
    /**
     * Mapa thread-safe para armazenar a memória de chat para cada sessão de usuário.
     * <p>
     * Utiliza {@link ConcurrentHashMap} para garantir operações thread-safe
     * em ambientes com múltiplas threads simultâneas. O mapa é inicializado
     * vazio e cresce conforme novas sessões são criadas.
     * <p>
     * <b>Thread safety:</b> Todas as operações (get, put, remove, clear) são
     * thread-safe e podem ser executadas concorrentemente.
     */
    private final ConcurrentHashMap<String, InMemoryChatMemory> sessionMemories = new ConcurrentHashMap<>();
    
    @Override
    public InMemoryChatMemory getOrCreate(String sessionId) {
        // Utiliza computeIfAbsent para implementar o padrão "get or create"
        // de forma thread-safe e eficiente
        return sessionMemories.computeIfAbsent(sessionId, id -> {
            log.debug("Criando nova memória de chat para a sessão: {}", id);
            return new InMemoryChatMemory();
        });
    }
    
    @Override
    public void clear(String sessionId) {
        // Remove a memória da sessão do mapa
        InMemoryChatMemory removed = sessionMemories.remove(sessionId);
        
        // Registra a operação apenas se uma memória foi realmente removida
        if (removed != null) {
            log.debug("Memória de chat removida para a sessão: {}", sessionId);
        }
    }
    
    @Override
    public void clearAll() {
        // Obtém o número de sessões antes da limpeza para logging
        int count = sessionMemories.size();
        
        // Remove todas as memórias de uma vez
        sessionMemories.clear();
        
        // Registra a operação com o número de sessões afetadas
        log.info("Todas as {} memórias de chat foram limpas", count);
    }
} 
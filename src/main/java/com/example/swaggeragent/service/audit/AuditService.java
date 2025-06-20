package com.example.swaggeragent.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de auditoria para logs estruturados em produção.
 * <p>
 * Esta classe fornece funcionalidades de auditoria para registrar eventos
 * importantes da aplicação de forma estruturada e consistente. Os logs
 * são formatados em JSON para facilitar a análise e processamento por
 * ferramentas de monitoramento e análise de logs.
 * <p>
 * <b>Tipos de eventos auditados:</b>
 * <ul>
 *   <li><b>Execução de ferramentas:</b> Chamadas a APIs externas com parâmetros e resultados</li>
 *   <li><b>Interações de chat:</b> Mensagens do usuário e respostas da IA</li>
 *   <li><b>Eventos de segurança:</b> Tentativas de acesso não autorizado, violações de segurança</li>
 * </ul>
 * <p>
 * <b>Características dos logs:</b>
 * <ul>
 *   <li><b>Formato JSON:</b> Estrutura consistente para facilitar parsing</li>
 *   <li><b>Timestamp ISO 8601:</b> Momento exato do evento</li>
 *   <li><b>Logger dedicado:</b> Usa logger "AUDIT" separado dos logs de aplicação</li>
 *   <li><b>Métricas de performance:</b> Duração das operações em milissegundos</li>
 *   <li><b>Contexto completo:</b> Inclui sessionId, parâmetros de entrada e saída</li>
 * </ul>
 * <p>
 * <b>Uso em produção:</b> Os logs de auditoria são essenciais para:
 * <ul>
 *   <li>Monitoramento de segurança e compliance</li>
 *   <li>Análise de performance e debugging</li>
 *   <li>Rastreamento de uso e comportamento dos usuários</li>
 *   <li>Investigação de incidentes</li>
 * </ul>
 */
@Service
public class AuditService {
    
    /**
     * Logger dedicado para eventos de auditoria.
     * <p>
     * Utiliza um logger separado com o nome "AUDIT" para facilitar
     * a configuração de níveis de log específicos para auditoria
     * e a separação dos logs de auditoria dos logs de aplicação.
     */
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    /**
     * Mapper JSON para serialização dos eventos de auditoria.
     * <p>
     * Utilizado para converter os objetos de evento em strings JSON
     * estruturadas que são registradas nos logs.
     */
    private final ObjectMapper objectMapper;
    
    /**
     * Constrói uma nova instância do serviço de auditoria.
     *
     * @param objectMapper o mapper JSON para serialização dos eventos
     */
    public AuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Registra a execução de uma ferramenta (chamada a API externa).
     * <p>
     * Este método registra informações detalhadas sobre a execução
     * de uma ferramenta dinâmica, incluindo:
     * <ul>
     *   <li>Nome da ferramenta executada</li>
     *   <li>ID da sessão do usuário</li>
     *   <li>Parâmetros de entrada (JSON)</li>
     *   <li>Resultado da execução (JSON)</li>
     *   <li>Duração da operação em milissegundos</li>
     *   <li>Timestamp do evento</li>
     * </ul>
     * <p>
     * <b>Exemplo de log:</b>
     * <pre>
     * AUDIT_EVENT: {
     *   "event_type": "TOOL_EXECUTION",
     *   "timestamp": "2024-01-15T10:30:00Z",
     *   "tool_name": "getUserById",
     *   "session_id": "sessao-123",
     *   "input": "{\"userId\": 123}",
     *   "output": "{\"id\": 123, \"name\": \"João\"}",
     *   "duration_ms": 150
     * }
     * </pre>
     *
     * @param toolName    o nome da ferramenta executada
     * @param sessionId   o ID da sessão do usuário
     * @param input       os parâmetros de entrada em formato JSON
     * @param output      o resultado da execução em formato JSON
     * @param durationMs  a duração da operação em milissegundos
     */
    public void logToolExecution(String toolName, String sessionId, String input, String output, long durationMs) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("event_type", "TOOL_EXECUTION");
        auditEvent.put("timestamp", Instant.now().toString());
        auditEvent.put("tool_name", toolName);
        auditEvent.put("session_id", sessionId);
        auditEvent.put("input", input);
        auditEvent.put("output", output);
        auditEvent.put("duration_ms", durationMs);
        
        logStructuredEvent(auditEvent);
    }
    
    /**
     * Registra uma interação de chat entre usuário e IA.
     * <p>
     * Este método registra informações sobre a comunicação entre
     * o usuário e o agente de IA, incluindo:
     * <ul>
     *   <li>ID da sessão do usuário</li>
     *   <li>Mensagem enviada pelo usuário</li>
     *   <li>Resposta gerada pela IA</li>
     *   <li>Duração do processamento em milissegundos</li>
     *   <li>Timestamp do evento</li>
     * </ul>
     * <p>
     * <b>Exemplo de log:</b>
     * <pre>
     * AUDIT_EVENT: {
     *   "event_type": "CHAT_INTERACTION",
     *   "timestamp": "2024-01-15T10:30:00Z",
     *   "session_id": "sessao-123",
     *   "user_message": "Qual é o status do pedido 123?",
     *   "ai_response": "O pedido 123 está em processamento...",
     *   "duration_ms": 2500
     * }
     * </pre>
     *
     * @param sessionId    o ID da sessão do usuário
     * @param userMessage  a mensagem enviada pelo usuário
     * @param aiResponse   a resposta gerada pela IA
     * @param durationMs   a duração do processamento em milissegundos
     */
    public void logChatInteraction(String sessionId, String userMessage, String aiResponse, long durationMs) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("event_type", "CHAT_INTERACTION");
        auditEvent.put("timestamp", Instant.now().toString());
        auditEvent.put("session_id", sessionId);
        auditEvent.put("user_message", userMessage);
        auditEvent.put("ai_response", aiResponse);
        auditEvent.put("duration_ms", durationMs);
        
        logStructuredEvent(auditEvent);
    }
    
    /**
     * Registra um evento de segurança.
     * <p>
     * Este método registra eventos relacionados à segurança da aplicação,
     * como tentativas de acesso não autorizado, violações de políticas,
     * ou outros incidentes de segurança.
     * <p>
     * <b>Tipos de eventos de segurança:</b>
     * <ul>
     *   <li><b>AUTH_FAILURE:</b> Falha de autenticação</li>
     *   <li><b>UNAUTHORIZED_ACCESS:</b> Tentativa de acesso não autorizado</li>
     *   <li><b>RATE_LIMIT_EXCEEDED:</b> Limite de requisições excedido</li>
     *   <li><b>INVALID_INPUT:</b> Dados de entrada maliciosos ou inválidos</li>
     *   <li><b>SYSTEM_ERROR:</b> Erros de sistema que podem indicar problemas de segurança</li>
     * </ul>
     * <p>
     * <b>Exemplo de log:</b>
     * <pre>
     * AUDIT_EVENT: {
     *   "event_type": "SECURITY_EVENT",
     *   "security_event_type": "UNAUTHORIZED_ACCESS",
     *   "timestamp": "2024-01-15T10:30:00Z",
     *   "session_id": "sessao-123",
     *   "details": "Tentativa de acesso a endpoint restrito sem autenticação"
     * }
     * </pre>
     *
     * @param eventType o tipo do evento de segurança
     * @param sessionId o ID da sessão (pode ser null se não aplicável)
     * @param details   detalhes específicos do evento de segurança
     */
    public void logSecurityEvent(String eventType, String sessionId, String details) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("event_type", "SECURITY_EVENT");
        auditEvent.put("security_event_type", eventType);
        auditEvent.put("timestamp", Instant.now().toString());
        auditEvent.put("session_id", sessionId);
        auditEvent.put("details", details);
        
        logStructuredEvent(auditEvent);
    }
    
    /**
     * Registra um evento estruturado no log de auditoria.
     * <p>
     * Este método privado é responsável por serializar o evento
     * em formato JSON e registrá-lo no logger de auditoria.
     * <p>
     * <b>Tratamento de erros:</b> Se houver erro na serialização JSON,
     * o erro é registrado no log de auditoria, mas não interrompe
     * o fluxo da aplicação.
     *
     * @param event o mapa contendo os dados do evento a ser registrado
     */
    private void logStructuredEvent(Map<String, Object> event) {
        try {
            // Serializa o evento em formato JSON
            String jsonEvent = objectMapper.writeValueAsString(event);
            
            // Registra o evento no logger de auditoria
            auditLogger.info("AUDIT_EVENT: {}", jsonEvent);
        } catch (Exception e) {
            // Em caso de erro na serialização, registra o erro mas não falha
            auditLogger.error("Erro ao serializar evento de auditoria: {}", event, e);
        }
    }
} 
package com.example.swaggeragent.service;

import org.springframework.stereotype.Service;

import com.example.swaggeragent.model.DynamicTool;
import com.example.swaggeragent.model.ToolMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerar prompts otimizados para o sistema de IA.
 * Cria prompts inteligentes que se adaptam ao número de ferramentas disponíveis.
 */
@Service
public class SystemPromptService {

    private final ToolLoggerService toolLoggerService;

    public SystemPromptService(ToolLoggerService toolLoggerService) {
        this.toolLoggerService = toolLoggerService;
    }

    /**
     * Gera um prompt do sistema otimizado baseado nas ferramentas disponíveis.
     * Para muitas ferramentas, usa um resumo compacto para evitar prompts muito longos.
     * 
     * @param tools Lista de ferramentas dinâmicas
     * @return Prompt do sistema otimizado
     */
    public String generateSystemPrompt(List<DynamicTool> tools) {
        StringBuilder prompt = new StringBuilder();

        // Cabeçalho do prompt
        prompt.append("Você é um assistente de IA especializado em executar operações através de APIs RESTful. ");
        prompt.append("Você tem acesso a ferramentas que correspondem a endpoints de diferentes microserviços organizados por projetos. ");
        prompt.append("Sempre que o usuário solicitar uma operação, analise qual ferramenta é mais apropriada e execute-a com precisão.\n\n");

        if (tools == null || tools.isEmpty()) {
            prompt.append("⚠️ ATENÇÃO: Nenhuma ferramenta está disponível no momento. ");
            prompt.append("Verifique se os arquivos OpenAPI estão presentes na pasta openapi-specs.\n\n");
        } else {
            // Decide entre prompt detalhado ou resumido baseado no número de ferramentas
            if (tools.size() <= 10) {
                prompt.append(generateDetailedToolsSection(tools));
            } else {
                prompt.append(generateCompactToolsSection(tools));
            }
        }

        // Instruções de comportamento
        prompt.append(generateBehaviorInstructions());
        
        // Instruções sobre formato de resposta
        prompt.append(generateResponseFormatInstructions());

        return prompt.toString();
    }

    /**
     * Gera seção detalhada de ferramentas para poucos endpoints.
     */
    private String generateDetailedToolsSection(List<DynamicTool> tools) {
        StringBuilder section = new StringBuilder();
        
        section.append("🛠️ FERRAMENTAS DISPONÍVEIS:\n\n");
        
        // Agrupa ferramentas por projeto
        Map<String, List<DynamicTool>> toolsByProject = tools.stream()
                .collect(Collectors.groupingBy(tool -> extractProjectName(tool.getEndpoint().baseUrl())));
        
        toolsByProject.forEach((project, projectTools) -> {
            section.append(String.format("📁 **%s API**:\n", project.toUpperCase()));
            
            for (DynamicTool tool : projectTools) {
                section.append(String.format("- **%s**: %s\n", tool.getName(), tool.getDescription()));
                section.append(String.format("  - Endpoint: `%s %s`\n", 
                        tool.getEndpoint().method().toUpperCase(), 
                        tool.getEndpoint().path()));
                
                if (tool.getEndpoint().parameters() != null && !tool.getEndpoint().parameters().isEmpty()) {
                    section.append("  - Parâmetros: ");
                    List<String> paramDescriptions = tool.getEndpoint().parameters().stream()
                            .map(p -> String.format("%s (%s)%s", 
                                    p.name(), 
                                    p.type(), 
                                    p.required() ? " [OBRIGATÓRIO]" : " [OPCIONAL]"))
                            .toList();
                    section.append(String.join(", ", paramDescriptions));
                    section.append("\n");
                }
                section.append("\n");
            }
            section.append("\n");
        });
        
        return section.toString();
    }

    /**
     * Gera seção compacta de ferramentas para muitos endpoints.
     */
    private String generateCompactToolsSection(List<DynamicTool> tools) {
        StringBuilder section = new StringBuilder();
        
        section.append("🛠️ FERRAMENTAS DISPONÍVEIS (").append(tools.size()).append(" ferramentas):\n\n");
        section.append("Devido ao grande número de ferramentas, aqui está um resumo organizado por projeto:\n\n");
        
        // Usa o resumo do ToolLoggerService
        String toolsSummary = toolLoggerService.getToolsSummaryForPrompt();
        section.append(toolsSummary);
        
        section.append("\n💡 **Dica**: Para obter detalhes específicos sobre qualquer ferramenta, ");
        section.append("pergunte sobre ela e eu fornecerei informações detalhadas.\n\n");
        
        return section.toString();
    }

    /**
     * Gera instruções de comportamento para a IA.
     */
    private String generateBehaviorInstructions() {
        StringBuilder instructions = new StringBuilder();
        
        instructions.append("📋 **INSTRUÇÕES DE COMPORTAMENTO**:\n");
        instructions.append("- Sempre explique qual ferramenta você está usando e por quê\n");
        instructions.append("- Se uma operação falhar, explique o erro e sugira alternativas quando possível\n");
        instructions.append("- Para perguntas sobre ferramentas disponíveis, organize a resposta por projeto\n");
        instructions.append("- Use emojis apropriados para tornar as respostas mais amigáveis\n");
        instructions.append("- Formate todas as respostas usando Markdown para clareza\n");
        instructions.append("- Quando o usuário perguntar sobre objetos que endpoints retornam, ");
        instructions.append("consulte os metadados das ferramentas para fornecer informações precisas\n\n");
        
        return instructions.toString();
    }

    /**
     * Gera instruções sobre formato de resposta das ferramentas.
     */
    private String generateResponseFormatInstructions() {
        StringBuilder instructions = new StringBuilder();
        
        instructions.append("⚙️ **FORMATO DE RESPOSTA DAS FERRAMENTAS**:\n");
        instructions.append("Cada ferramenta retorna um objeto JSON com:\n");
        instructions.append("- `httpStatusCode`: Código de status HTTP numérico\n");
        instructions.append("- `body`: Corpo da resposta (JSON ou texto)\n\n");
        
        instructions.append("**Interpretação dos códigos de status**:\n");
        instructions.append("- 2xx: ✅ Sucesso\n");
        instructions.append("- 4xx: ⚠️ Erro do cliente (dados inválidos, não encontrado, etc.)\n");
        instructions.append("- 5xx: ❌ Erro do servidor\n\n");
        
        instructions.append("Sempre baseie sua resposta final no `httpStatusCode` e `body` recebidos, ");
        instructions.append("explicando o resultado de forma clara e acionável para o usuário.\n");
        
        return instructions.toString();
    }

    /**
     * Extrai o nome do projeto da URL base.
     */
    private String extractProjectName(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "api";
        }
        
        try {
            if (baseUrl.contains(":")) {
                String[] parts = baseUrl.split(":");
                if (parts.length >= 3) {
                    String port = parts[2].replaceAll("[^0-9]", "");
                    return switch (port) {
                        case "3000" -> "cards";
                        case "3001" -> "invoices";
                        case "3002" -> "proposals";
                        default -> "api_" + port;
                    };
                }
            }
            return "api";
        } catch (Exception e) {
            return "api";
        }
    }
} 
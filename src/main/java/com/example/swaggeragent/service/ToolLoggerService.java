package com.example.swaggeragent.service;

import com.example.swaggeragent.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serviço responsável por fazer o logging detalhado das ferramentas geradas.
 * Captura informações de request/response e mantém metadados para otimização do prompt.
 */
@Service
public class ToolLoggerService {

    private final ObjectMapper objectMapper;
    private final Map<String, ToolMetadata> toolMetadataCache = new ConcurrentHashMap<>();

    public ToolLoggerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Faz o log das ferramentas geradas e armazena metadados para uso posterior.
     * 
     * @param tools Lista de ferramentas dinâmicas
     */
    public void logTools(List<DynamicTool> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }

        System.out.println("\n====================================================================================================");
        System.out.println("                           🛠️  Ferramentas Dinâmicas Carregadas 🛠️");
        System.out.println("====================================================================================================");

        for (int i = 0; i < tools.size(); i++) {
            DynamicTool tool = tools.get(i);
            OpenApiEndpoint endpoint = tool.getEndpoint();

            // Armazena metadados da ferramenta
            ToolMetadata metadata = extractToolMetadata(tool);
            toolMetadataCache.put(tool.getName(), metadata);

            System.out.printf("%n----------------------------------------- Ferramenta %d de %d --------------------------------------------%n", i + 1, tools.size());
            System.out.printf("🏷️  Nome: %s%n", tool.getName());
            System.out.printf("📝 Descrição: %s%n", tool.getDescription());
            System.out.printf("🆔 ID da Operação: %s%n", endpoint.operationId());
            System.out.printf("🌐 Projeto: %s%n", metadata.getProjectName());
            System.out.printf("🎯 Controller: %s%n", metadata.getControllerName());
            System.out.printf("   %s %s%s%n", endpoint.method().toUpperCase(), endpoint.baseUrl(), endpoint.path());
            System.out.println("------------------------------------------- ENTRADAS ---------------------------------------------");

            logParameters(endpoint.parameters());
            logRequestBody(endpoint.requestBody());

            System.out.println("------------------------------------------- SAÍDAS --------------------------------------------");
            logResponses(endpoint.responses());
            
            // Log de exemplos se disponíveis
            if (metadata.getRequestExample() != null) {
                System.out.println("----------------------------------------- EXEMPLO REQUEST -----------------------------------------");
                System.out.println(metadata.getRequestExample());
            }
            
            if (metadata.getResponseExample() != null) {
                System.out.println("---------------------------------------- EXEMPLO RESPONSE ----------------------------------------");
                System.out.println(metadata.getResponseExample());
            }
            
            System.out.println("----------------------------------------------------------------------------------------------------");
        }
        System.out.printf("%n====================================== %d Ferramentas Carregadas com Sucesso ======================================%n%n", tools.size());
    }

    /**
     * Extrai metadados detalhados de uma ferramenta para cache e otimização.
     * 
     * @param tool Ferramenta dinâmica
     * @return Metadados extraídos
     */
    private ToolMetadata extractToolMetadata(DynamicTool tool) {
        OpenApiEndpoint endpoint = tool.getEndpoint();
        
        // Extrai nome do projeto da URL base
        String projectName = extractProjectNameFromUrl(endpoint.baseUrl());
        
        // Extrai nome do controller das tags
        String controllerName = endpoint.tags() != null && !endpoint.tags().isEmpty() 
                ? endpoint.tags().get(0) : "general";
        
        // Extrai exemplos de request e response
        String requestExample = extractRequestExample(endpoint);
        String responseExample = extractResponseExample(endpoint);
        
        // Extrai schemas
        String requestSchema = extractRequestSchema(endpoint);
        String responseSchema = extractResponseSchema(endpoint);
        
        return new ToolMetadata(
                tool.getName(),
                projectName,
                controllerName,
                endpoint.operationId(),
                requestExample,
                responseExample,
                requestSchema,
                responseSchema
        );
    }

    /**
     * Extrai o nome do projeto da URL base.
     */
    private String extractProjectNameFromUrl(String baseUrl) {
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

    /**
     * Extrai exemplo de request do endpoint.
     */
    private String extractRequestExample(OpenApiEndpoint endpoint) {
        if (endpoint.requestBody() != null && endpoint.requestBody().content() != null) {
            return endpoint.requestBody().content().values().stream()
                    .filter(mediaType -> mediaType.example() != null)
                    .map(mediaType -> {
                        try {
                            return objectMapper.writeValueAsString(mediaType.example());
                        } catch (Exception e) {
                            return mediaType.example().toString();
                        }
                    })
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Extrai exemplo de response do endpoint.
     */
    private String extractResponseExample(OpenApiEndpoint endpoint) {
        return endpoint.responses().values().stream()
                .filter(response -> response.content() != null)
                .flatMap(response -> response.content().values().stream())
                .filter(mediaType -> mediaType.example() != null)
                .map(mediaType -> {
                    try {
                        return objectMapper.writeValueAsString(mediaType.example());
                    } catch (Exception e) {
                        return mediaType.example().toString();
                    }
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Extrai schema de request do endpoint.
     */
    private String extractRequestSchema(OpenApiEndpoint endpoint) {
        if (endpoint.requestBody() != null && endpoint.requestBody().content() != null) {
            return endpoint.requestBody().content().values().stream()
                    .filter(mediaType -> mediaType.schema() != null)
                    .map(mediaType -> {
                        if (mediaType.schema().get$ref() != null) {
                            return mediaType.schema().get$ref();
                        }
                        try {
                            return objectMapper.writeValueAsString(mediaType.schema());
                        } catch (Exception e) {
                            return "Schema não disponível";
                        }
                    })
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Extrai schema de response do endpoint.
     */
    private String extractResponseSchema(OpenApiEndpoint endpoint) {
        return endpoint.responses().values().stream()
                .filter(response -> response.content() != null)
                .flatMap(response -> response.content().values().stream())
                .filter(mediaType -> mediaType.schema() != null)
                .map(mediaType -> {
                    if (mediaType.schema().get$ref() != null) {
                        return mediaType.schema().get$ref();
                    }
                    try {
                        return objectMapper.writeValueAsString(mediaType.schema());
                    } catch (Exception e) {
                        return "Schema não disponível";
                    }
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Retorna um resumo compacto de todas as ferramentas para uso no prompt da IA.
     * 
     * @return String com resumo das ferramentas
     */
    public String getToolsSummaryForPrompt() {
        if (toolMetadataCache.isEmpty()) {
            return "Nenhuma ferramenta disponível.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Ferramentas disponíveis:\n");
        
        // Agrupa por projeto para melhor organização
        Map<String, List<ToolMetadata>> toolsByProject = toolMetadataCache.values().stream()
                .collect(Collectors.groupingBy(ToolMetadata::getProjectName));
        
        toolsByProject.forEach((project, tools) -> {
            summary.append(String.format("\n[%s API]:\n", project.toUpperCase()));
            tools.forEach(tool -> {
                summary.append(String.format("- %s: %s\n", 
                        tool.getToolName(), 
                        tool.generateCompactSummary()));
            });
        });
        
        return summary.toString();
    }

    /**
     * Retorna metadados de uma ferramenta específica.
     * 
     * @param toolName Nome da ferramenta
     * @return Metadados da ferramenta ou null se não encontrada
     */
    public ToolMetadata getToolMetadata(String toolName) {
        return toolMetadataCache.get(toolName);
    }

    /**
     * Faz o log dos parâmetros de um endpoint.
     * 
     * @param parameters Lista de parâmetros
     */
    private void logParameters(List<OpenApiParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            System.out.println("  Nenhum parâmetro definido.");
            return;
        }

        Map<String, List<OpenApiParameter>> groupedParameters = parameters.stream()
                .collect(Collectors.groupingBy(OpenApiParameter::in));

        logGroupedParameters("Path", groupedParameters.get("path"));
        logGroupedParameters("Query", groupedParameters.get("query"));
        logGroupedParameters("Header", groupedParameters.get("header"));
    }

    /**
     * Faz o log de parâmetros agrupados por tipo.
     * 
     * @param groupName Nome do grupo (Path, Query, Header)
     * @param params Lista de parâmetros do grupo
     */
    private void logGroupedParameters(String groupName, List<OpenApiParameter> params) {
        if (params != null && !params.isEmpty()) {
            System.out.printf("  📥 Parâmetros %s:%n", groupName);
            for (OpenApiParameter param : params) {
                String requiredInfo = param.required() ? "OBRIGATÓRIO" : "OPCIONAL";
                System.out.printf("     - %s (%s) [%s]: %s%n", param.name(), param.type(), requiredInfo, param.description());

                if (param.format() != null) {
                    System.out.printf("       Formato: %s%n", param.format());
                }
                if (param.defaultValue() != null) {
                    System.out.printf("       Padrão: %s%n", param.defaultValue());
                }
                if (param.enumValues() != null && !param.enumValues().isEmpty()) {
                    System.out.printf("       Valores Enum: %s%n", param.enumValues());
                }
                if (param.items() != null) {
                    System.out.printf("       Tipo dos Items: %s%n", param.items().type());
                }
            }
        }
    }

    /**
     * Faz o log do corpo da requisição.
     * 
     * @param requestBody Corpo da requisição
     */
    private void logRequestBody(OpenApiRequestBody requestBody) {
        if (requestBody != null && requestBody.content() != null && !requestBody.content().isEmpty()) {
            System.out.println("  📦 Corpo da Requisição:");
            System.out.printf("     - Obrigatório: %s%n", requestBody.required() ? "Sim" : "Não");
            if (requestBody.description() != null && !requestBody.description().isBlank()) {
                System.out.printf("     - Descrição: %s%n", requestBody.description());
            }
            requestBody.content().forEach((mediaType, mediaTypeObject) -> {
                System.out.printf("     - Tipo de Mídia: %s%n", mediaType);
                if (mediaTypeObject.schema() != null && mediaTypeObject.schema().get$ref() != null) {
                    System.out.printf("       Schema: %s%n", mediaTypeObject.schema().get$ref());
                }
                if (mediaTypeObject.example() != null) {
                    System.out.printf("       Exemplo: %s%n", mediaTypeObject.example().toString());
                }
            });
        }
    }

    /**
     * Faz o log das respostas de um endpoint.
     * 
     * @param responses Mapa de respostas por código de status
     */
    private void logResponses(Map<String, OpenApiResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("  Nenhuma resposta definida.");
            return;
        }
        System.out.println("  📤 Respostas:");
        responses.forEach((statusCode, response) -> {
            System.out.printf("  - HTTP %s: %s%n", statusCode, response.description());
            if (response.content() != null) {
                response.content().forEach((mediaType, mediaTypeObject) -> {
                    System.out.printf("    - Tipo de Conteúdo: %s%n", mediaType);
                    if (mediaTypeObject.schema() != null && mediaTypeObject.schema().get$ref() != null) {
                        System.out.printf("      Schema: %s%n", mediaTypeObject.schema().get$ref());
                    }
                    if (mediaTypeObject.example() != null) {
                        System.out.printf("      Exemplo: %s%n", mediaTypeObject.example().toString());
                    }
                });
            }
        });
    }
} 
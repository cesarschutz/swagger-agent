package com.example.swaggeragent.model;

/**
 * Representa informações detalhadas sobre uma ferramenta, incluindo
 * exemplos de request e response para melhor contexto da IA.
 */
public class ToolMetadata {
    private String toolName;
    private String projectName;
    private String controllerName;
    private String operationId;
    private String requestExample;
    private String responseExample;
    private String requestSchema;
    private String responseSchema;

    public ToolMetadata() {
    }

    public ToolMetadata(String toolName, String projectName, String controllerName, 
                       String operationId, String requestExample, String responseExample,
                       String requestSchema, String responseSchema) {
        this.toolName = toolName;
        this.projectName = projectName;
        this.controllerName = controllerName;
        this.operationId = operationId;
        this.requestExample = requestExample;
        this.responseExample = responseExample;
        this.requestSchema = requestSchema;
        this.responseSchema = responseSchema;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getRequestExample() {
        return requestExample;
    }

    public void setRequestExample(String requestExample) {
        this.requestExample = requestExample;
    }

    public String getResponseExample() {
        return responseExample;
    }

    public void setResponseExample(String responseExample) {
        this.responseExample = responseExample;
    }

    public String getRequestSchema() {
        return requestSchema;
    }

    public void setRequestSchema(String requestSchema) {
        this.requestSchema = requestSchema;
    }

    public String getResponseSchema() {
        return responseSchema;
    }

    public void setResponseSchema(String responseSchema) {
        this.responseSchema = responseSchema;
    }

    /**
     * Gera um resumo compacto das informações da ferramenta para o prompt da IA.
     * 
     * @return String formatada com informações essenciais
     */
    public String generateCompactSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Tool: %s | Project: %s | Controller: %s", 
                toolName, projectName, controllerName));
        
        if (requestExample != null && !requestExample.isEmpty()) {
            summary.append(" | Request: ").append(requestExample);
        }
        
        if (responseExample != null && !responseExample.isEmpty()) {
            summary.append(" | Response: ").append(responseExample);
        }
        
        return summary.toString();
    }
}


package com.example.swaggeragent.dto;

import com.example.swaggeragent.model.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO para resposta das ferramentas disponíveis.
 */
public class ToolsResponse {
    private int totalTools;
    private Map<String, List<ToolInfo>> toolsByProject;

    public ToolsResponse(List<DynamicTool> tools) {
        this.totalTools = tools != null ? tools.size() : 0;
        this.toolsByProject = tools != null ? groupToolsByProject(tools) : Map.of();
    }

    private Map<String, List<ToolInfo>> groupToolsByProject(List<DynamicTool> tools) {
        return tools.stream()
                .collect(Collectors.groupingBy(
                        tool -> extractProjectName(tool.getEndpoint().baseUrl()),
                        Collectors.mapping(ToolInfo::new, Collectors.toList())
                ));
    }

    private String extractProjectName(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "API Geral";
        }
        
        try {
            if (baseUrl.contains(":")) {
                String[] parts = baseUrl.split(":");
                if (parts.length >= 3) {
                    String port = parts[2].replaceAll("[^0-9]", "");
                    return switch (port) {
                        case "3000" -> "Cards API";
                        case "3001" -> "Invoices API";
                        case "3002" -> "Proposals API";
                        default -> "API " + port;
                    };
                }
            }
            return "API Geral";
        } catch (Exception e) {
            return "API Geral";
        }
    }

    // Getters e Setters
    public int getTotalTools() {
        return totalTools;
    }

    public void setTotalTools(int totalTools) {
        this.totalTools = totalTools;
    }

    public Map<String, List<ToolInfo>> getToolsByProject() {
        return toolsByProject;
    }

    public void setToolsByProject(Map<String, List<ToolInfo>> toolsByProject) {
        this.toolsByProject = toolsByProject;
    }

    /**
     * Classe interna para informações de uma ferramenta.
     */
    public static class ToolInfo {
        private String name;
        private String description;
        private String method;
        private String path;
        private String controller;
        private List<ParameterInfo> parameters;

        public ToolInfo(DynamicTool tool) {
            OpenApiEndpoint endpoint = tool.getEndpoint();
            this.name = tool.getName();
            this.description = tool.getDescription();
            this.method = endpoint.method().toUpperCase();
            this.path = endpoint.path();
            this.controller = endpoint.tags() != null && !endpoint.tags().isEmpty() 
                    ? endpoint.tags().get(0) : "general";
            this.parameters = endpoint.parameters() != null 
                    ? endpoint.parameters().stream()
                            .map(ParameterInfo::new)
                            .collect(Collectors.toList())
                    : List.of();
        }

        // Getters e Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getController() {
            return controller;
        }

        public void setController(String controller) {
            this.controller = controller;
        }

        public List<ParameterInfo> getParameters() {
            return parameters;
        }

        public void setParameters(List<ParameterInfo> parameters) {
            this.parameters = parameters;
        }
    }

    /**
     * Classe interna para informações de um parâmetro.
     */
    public static class ParameterInfo {
        private String name;
        private String type;
        private String location;
        private boolean required;
        private String description;

        public ParameterInfo(com.example.swaggeragent.model.OpenApiParameter param) {
            this.name = param.name();
            this.type = param.type();
            this.location = param.in();
            this.required = param.required();
            this.description = param.description();
        }

        // Getters e Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}


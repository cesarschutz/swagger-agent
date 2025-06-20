package com.example.swaggeragent.dto;

import com.example.swaggeragent.model.domain.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO (Data Transfer Object) para a resposta do endpoint que lista as ferramentas disponíveis.
 * <p>
 * Agrupa as ferramentas por projeto e fornece um totalizador. Esta estrutura facilita
 * a visualização e organização das ferramentas na interface do usuário.
 */
public class ToolsResponse {
    /**
     * O número total de ferramentas disponíveis.
     */
    private int totalTools;

    /**
     * Um mapa onde a chave é o nome do projeto e o valor é uma lista de informações
     * sobre as ferramentas daquele projeto.
     */
    private Map<String, List<ToolInfo>> toolsByProject;

    /**
     * Construtor que transforma uma lista de {@link DynamicTool} na estrutura de resposta.
     *
     * @param tools a lista de ferramentas dinâmicas a serem processadas.
     */
    public ToolsResponse(List<DynamicTool> tools) {
        this.totalTools = tools != null ? tools.size() : 0;
        this.toolsByProject = tools != null ? groupToolsByProject(tools) : Map.of();
    }

    /**
     * Agrupa as ferramentas por projeto.
     * <p>
     * Este método privado converte a lista de {@link DynamicTool} em um mapa,
     * utilizando o nome do projeto como chave.
     *
     * @param tools a lista de ferramentas a serem agrupadas.
     * @return um mapa de ferramentas agrupadas por projeto.
     */
    private Map<String, List<ToolInfo>> groupToolsByProject(List<DynamicTool> tools) {
        return tools.stream()
                .collect(Collectors.groupingBy(
                        tool -> tool.getEndpoint().projectName(),
                        Collectors.mapping(ToolInfo::new, Collectors.toList())
                ));
    }

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
     * Classe interna que representa as informações detalhadas de uma única ferramenta.
     * <p>
     * É usada para serializar os dados relevantes de uma {@link DynamicTool} de forma
     * amigável para o frontend.
     */
    public static class ToolInfo {
        private String name;
        private String description;
        private String method;
        private String path;
        private String baseUrl;
        private String controller;
        private List<ParameterInfo> parameters;

        /**
         * Construtor que extrai informações de uma {@link DynamicTool}.
         *
         * @param tool a ferramenta da qual extrair as informações.
         */
        public ToolInfo(DynamicTool tool) {
            OpenApiEndpoint endpoint = tool.getEndpoint();
            this.name = tool.getName();
            this.description = tool.getSummary();
            this.method = endpoint.method().toUpperCase();
            this.path = endpoint.path();
            this.baseUrl = endpoint.baseUrl();
            this.controller = endpoint.tags() != null && !endpoint.tags().isEmpty()
                    ? endpoint.tags().get(0) : "general";
            this.parameters = endpoint.parameters() != null
                    ? endpoint.parameters().stream()
                    .map(ParameterInfo::new)
                    .collect(Collectors.toList())
                    : List.of();
        }

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

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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
     * Classe interna que representa as informações de um parâmetro de uma ferramenta.
     */
    public static class ParameterInfo {
        private String name;
        private String type;
        private String location;
        private boolean required;
        private String description;

        /**
         * Construtor que extrai informações de um parâmetro OpenAPI.
         *
         * @param param o parâmetro OpenAPI do qual extrair as informações.
         */
        public ParameterInfo(com.example.swaggeragent.model.OpenApiParameter param) {
            this.name = param.name();
            this.type = param.type();
            this.location = param.in();
            this.required = param.required();
            this.description = param.description();
        }

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


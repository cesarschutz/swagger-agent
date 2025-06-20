package com.example.swaggeragent.model.domain;

import java.util.function.Function;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiParameter;

/**
 * Representa uma ferramenta (tool) dinâmica gerada a partir de uma especificação OpenAPI.
 * <p>
 * Uma ferramenta encapsula a lógica para executar uma chamada a um endpoint de API,
 * incluindo seu nome, descrição, o endpoint em si, a função de execução e o esquema JSON
 * para seus parâmetros.
 */
public class DynamicTool {

    /**
     * O nome único da ferramenta, geralmente derivado do `operationId` do OpenAPI.
     */
    private String name;

    /**
     * Uma descrição detalhada do que a ferramenta faz.
     */
    private String description;

    /**
     * Um resumo conciso da funcionalidade da ferramenta.
     */
    private String summary;

    /**
     * O objeto que representa o endpoint da API, contendo informações como path, método, etc.
     */
    private OpenApiEndpoint endpoint;

    /**
     * A função Java que, quando chamada com os argumentos corretos, executa a chamada à API.
     */
    private Function<Object, String> function;

    /**
     * O esquema JSON que define a estrutura dos parâmetros da ferramenta.
     */
    private String jsonSchema;

    /**
     * Construtor padrão.
     */
    public DynamicTool() {
    }

    /**
     * Construtor completo para criar uma instância de {@code DynamicTool}.
     *
     * @param name        o nome da ferramenta.
     * @param description a descrição da ferramenta.
     * @param summary     o resumo da ferramenta.
     * @param endpoint    o endpoint da API.
     * @param function    a função de execução.
     * @param jsonSchema  o esquema JSON dos parâmetros.
     */
    public DynamicTool(String name, String description, String summary, OpenApiEndpoint endpoint, Function<Object, String> function, String jsonSchema) {
        this.name = name;
        this.description = description;
        this.summary = summary;
        this.endpoint = endpoint;
        this.function = function;
        this.jsonSchema = jsonSchema;
    }

    /**
     * Retorna uma nova instância do Builder para construir uma {@code DynamicTool}.
     *
     * @return uma nova instância de {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gera uma descrição textual e formatada da ferramenta, incluindo detalhes do endpoint
     * e seus parâmetros. Utiliza os termos "OBRIGATÓRIO" e "OPCIONAL" para clareza.
     *
     * @return uma string com a descrição completa da ferramenta.
     */
    public String getToolDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool: ").append(name).append("\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Method: ").append(endpoint.method().toUpperCase()).append("\n");
        sb.append("Path: ").append(endpoint.path()).append("\n");

        if (endpoint.parameters() != null && !endpoint.parameters().isEmpty()) {
            sb.append("Parameters:\n");
            for (OpenApiParameter param : endpoint.parameters()) {
                sb.append("  - ").append(param.name())
                        .append(" (").append(param.type()).append(")")
                        .append(param.required() ? " [OBRIGATÓRIO]" : " [OPCIONAL]")
                        .append(": ").append(param.description()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Obtém o nome da ferramenta.
     * @return o nome.
     */
    public String getName() {
        return name;
    }

    /**
     * Define o nome da ferramenta.
     * @param name o novo nome.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtém a descrição da ferramenta.
     * @return a descrição.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Define a descrição da ferramenta.
     * @param description a nova descrição.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Obtém o resumo da ferramenta.
     * @return o resumo.
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Define o resumo da ferramenta.
     * @param summary o novo resumo.
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Obtém o endpoint da API associado à ferramenta.
     * @return o endpoint.
     */
    public OpenApiEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Define o endpoint da API associado à ferramenta.
     * @param endpoint o novo endpoint.
     */
    public void setEndpoint(OpenApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Obtém a função de execução da ferramenta.
     * @return a função.
     */
    public Function<Object, String> getFunction() {
        return function;
    }

    /**
     * Define a função de execução da ferramenta.
     * @param function a nova função.
     */
    public void setFunction(Function<Object, String> function) {
        this.function = function;
    }

    /**
     * Obtém o esquema JSON dos parâmetros da ferramenta.
     * @return o esquema JSON.
     */
    public String getJsonSchema() {
        return jsonSchema;
    }

    /**
     * Define o esquema JSON dos parâmetros da ferramenta.
     * @param jsonSchema o novo esquema JSON.
     */
    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    /**
     * Classe Builder para facilitar a construção de instâncias de {@link DynamicTool}.
     */
    public static class Builder {
        private String name;
        private String description;
        private String summary;
        private OpenApiEndpoint endpoint;
        private Function<Object, String> function;
        private String jsonSchema;

        /**
         * Define o nome da ferramenta.
         * @param name o nome.
         * @return a própria instância do Builder para encadeamento.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Define a descrição da ferramenta.
         * @param description a descrição.
         * @return a própria instância do Builder para encadeamento.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Define o resumo da ferramenta.
         * @param summary o resumo.
         * @return a própria instância do Builder para encadeamento.
         */
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        /**
         * Define o endpoint da API.
         * @param endpoint o endpoint.
         * @return a própria instância do Builder para encadeamento.
         */
        public Builder endpoint(OpenApiEndpoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Define a função de execução.
         * @param function a função.
         * @return a própria instância do Builder para encadeamento.
         */
        public Builder function(Function<Object, String> function) {
            this.function = function;
            return this;
        }

        /**
         * Define o esquema JSON dos parâmetros.
         * @param jsonSchema o esquema JSON.
         * @return a própria instância do Builder para encadeamento.
         */
        public Builder jsonSchema(String jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        /**
         * Constrói e retorna uma nova instância de {@link DynamicTool} com os valores definidos.
         * @return uma nova instância de {@link DynamicTool}.
         */
        public DynamicTool build() {
            return new DynamicTool(name, description, summary, endpoint, function, jsonSchema);
        }
    }
} 
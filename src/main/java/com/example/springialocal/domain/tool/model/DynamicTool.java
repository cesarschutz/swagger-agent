package com.example.springialocal.domain.tool.model;

import java.util.function.Function;

public class DynamicTool {
    private String name;
    private String description;
    private OpenApiEndpoint endpoint;
    private Function<String, String> function;
    private String jsonSchema;

    public DynamicTool() {
    }

    public DynamicTool(String name, String description, OpenApiEndpoint endpoint, Function<String, String> function, String jsonSchema) {
        this.name = name;
        this.description = description;
        this.endpoint = endpoint;
        this.function = function;
        this.jsonSchema = jsonSchema;
    }

    public static Builder builder() {
        return new Builder();
    }

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
                  .append(param.required() ? " [REQUIRED]" : " [OPTIONAL]")
                  .append(": ").append(param.description()).append("\n");
            }
        }
        
        return sb.toString();
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

    public OpenApiEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(OpenApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Function<String, String> getFunction() {
        return function;
    }

    public void setFunction(Function<String, String> function) {
        this.function = function;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public static class Builder {
        private String name;
        private String description;
        private OpenApiEndpoint endpoint;
        private Function<String, String> function;
        private String jsonSchema;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder endpoint(OpenApiEndpoint endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder function(Function<String, String> function) {
            this.function = function;
            return this;
        }

        public Builder jsonSchema(String jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        public DynamicTool build() {
            return new DynamicTool(name, description, endpoint, function, jsonSchema);
        }
    }
}


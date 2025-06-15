package com.example.swaggeragent.service;

import com.example.swaggeragent.model.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.model.OpenApiParameter;
import com.example.swaggeragent.model.OpenApiRequestBody;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolLoggerService {

    public void logTools(List<DynamicTool> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }

        System.out.println("====================================================================================================");
        System.out.println("                           🛠️  Dynamically Generated Tools Loaded 🛠️");
        System.out.println("====================================================================================================");

        for (int i = 0; i < tools.size(); i++) {
            DynamicTool tool = tools.get(i);
            OpenApiEndpoint endpoint = tool.getEndpoint();

            System.out.printf("----------------------------------------- Tool %d of %d --------------------------------------------%n", i + 1, tools.size());
            System.out.printf("🏷️  Name: %s%n", tool.getName());
            System.out.printf("📝 Description: %s%n", tool.getDescription());
            System.out.println("----------------------------------------- Endpoint Details ---------------------------------------");
            System.out.printf("   - 🆔 Operation ID: %s%n", endpoint.operationId());
            System.out.printf("   - 🌐 Method: %s%n", endpoint.method().toUpperCase());
            System.out.printf("   - 🛤️ Path: %s%n", endpoint.path());
            System.out.printf("   - 📌 Base URL: %s%n", endpoint.baseUrl());
            System.out.printf("   - 📋 Summary: %s%n", endpoint.summary());

            if (endpoint.parameters() != null && !endpoint.parameters().isEmpty()) {
                System.out.println("   - 📥 Parameters:");
                for (OpenApiParameter param : endpoint.parameters()) {
                    System.out.printf("     - Name: %s (%s) %s%n", param.name(), param.type(), param.required() ? "[REQUIRED]" : "[OPTIONAL]");
                    System.out.printf("       Description: %s%n", param.description());
                }
            }

            OpenApiRequestBody requestBody = endpoint.requestBody();
            if (requestBody != null && requestBody.content() != null && !requestBody.content().isEmpty()) {
                System.out.println("   - 📦 Request Body:");
                System.out.printf("     - Description: %s%n", requestBody.description());
                System.out.printf("     - Required: %s%n", requestBody.required());
                requestBody.content().forEach((mediaType, mediaTypeObject) -> {
                    System.out.printf("     - Media Type: %s%n", mediaType);
                    System.out.printf("       Schema: %s%n", mediaTypeObject.schema());
                });
            }

            System.out.println("   - 📜 JSON Schema:");
            System.out.printf("     %s%n", tool.getJsonSchema());
            System.out.println("----------------------------------------------------------------------------------------------------");
        }
        System.out.printf("====================================== %d Tools Loaded Successfully ======================================%n", tools.size());
    }
} 
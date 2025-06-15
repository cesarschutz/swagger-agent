package com.example.swaggeragent.service;

import com.example.swaggeragent.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ToolLoggerService {

    public void logTools(List<DynamicTool> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }

        System.out.println("\n====================================================================================================");
        System.out.println("                           🛠️  Dynamically Generated Tools Loaded 🛠️");
        System.out.println("====================================================================================================");

        for (int i = 0; i < tools.size(); i++) {
            DynamicTool tool = tools.get(i);
            OpenApiEndpoint endpoint = tool.getEndpoint();

            System.out.printf("%n----------------------------------------- Tool %d of %d --------------------------------------------%n", i + 1, tools.size());
            System.out.printf("🏷️  Name: %s%n", tool.getName());
            System.out.printf("📝 Description: %s%n", tool.getDescription());
            System.out.printf("🆔 Operation ID: %s%n", endpoint.operationId());
            System.out.printf("   %s %s%s%n", endpoint.method().toUpperCase(), endpoint.baseUrl(), endpoint.path());
            System.out.println("------------------------------------------- INPUTS ---------------------------------------------");

            logParameters(endpoint.parameters());
            logRequestBody(endpoint.requestBody());

            System.out.println("------------------------------------------- OUTPUTS --------------------------------------------");
            logResponses(endpoint.responses());
            System.out.println("----------------------------------------------------------------------------------------------------");
        }
        System.out.printf("%n====================================== %d Tools Loaded Successfully ======================================%n%n", tools.size());
    }

    private void logParameters(List<OpenApiParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            System.out.println("  No parameters defined.");
            return;
        }

        Map<String, List<OpenApiParameter>> groupedParameters = parameters.stream()
                .collect(Collectors.groupingBy(OpenApiParameter::in));

        logGroupedParameters("Path", groupedParameters.get("path"));
        logGroupedParameters("Query", groupedParameters.get("query"));
        logGroupedParameters("Header", groupedParameters.get("header"));
    }

    private void logGroupedParameters(String groupName, List<OpenApiParameter> params) {
        if (params != null && !params.isEmpty()) {
            System.out.printf("  📥 %s Parameters:%n", groupName);
            for (OpenApiParameter param : params) {
                System.out.printf("     - %s (%s) %s: %s%n",
                        param.name(),
                        param.type(),
                        param.required() ? "[REQUIRED]" : "[OPTIONAL]",
                        param.description());
            }
        }
    }

    private void logRequestBody(OpenApiRequestBody requestBody) {
        if (requestBody != null && requestBody.content() != null && !requestBody.content().isEmpty()) {
            System.out.println("  📦 Request Body:");
            System.out.printf("     - Required: %s%n", requestBody.required());
            if (requestBody.description() != null && !requestBody.description().isBlank()) {
                System.out.printf("     - Description: %s%n", requestBody.description());
            }
            requestBody.content().forEach((mediaType, mediaTypeObject) -> {
                System.out.printf("     - Media Type: %s%n", mediaType);
                if (mediaTypeObject.schema() != null) {
                    System.out.printf("       Schema: %s%n", mediaTypeObject.schema().get$ref());
                }
            });
        }
    }

    private void logResponses(Map<String, OpenApiResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("  No responses defined.");
            return;
        }
        System.out.println("  📤 Responses:");
        responses.forEach((statusCode, response) -> {
            System.out.printf("  - HTTP %s: %s%n", statusCode, response.description());
            if (response.content() != null) {
                response.content().forEach((mediaType, mediaTypeObject) -> {
                    System.out.printf("    - Content-Type: %s%n", mediaType);
                    if (mediaTypeObject.schema() != null && mediaTypeObject.schema().get$ref() != null) {
                        System.out.printf("      Schema: %s%n", mediaTypeObject.schema().get$ref());
                    }
                });
            }
        });
    }
} 
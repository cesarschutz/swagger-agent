package com.example.swaggeragent.service;

import org.springframework.stereotype.Service;

import com.example.swaggeragent.model.DynamicTool;

import java.util.List;

@Service
public class SystemPromptService {

    public String generateSystemPrompt(List<DynamicTool> tools) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Você é um assistente AI especializado em executar operações através de APIs. ");
        prompt.append("Você tem acesso a várias ferramentas que correspondem a endpoints de diferentes microserviços. ");
        prompt.append("Sempre que o usuário solicitar uma operação, analise qual ferramenta é mais apropriada e execute-a. ");
        prompt.append("Seja preciso e forneça respostas claras sobre os resultados das operações.\n\n");

        if (tools != null && !tools.isEmpty()) {
            prompt.append("FERRAMENTAS DISPONÍVEIS:\n");
            for (DynamicTool tool : tools) {
                prompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
                prompt.append("  Endpoint: ").append(tool.getEndpoint().method().toUpperCase())
                        .append(" ").append(tool.getEndpoint().path()).append("\n");

                if (tool.getEndpoint().parameters() != null && !tool.getEndpoint().parameters().isEmpty()) {
                    prompt.append("  Parâmetros: ");
                    List<String> paramDescriptions = tool.getEndpoint().parameters().stream()
                            .map(p -> p.name() + " (" + p.type() + ")" + (p.required() ? " [OBRIGATÓRIO]" : " [OPCIONAL]"))
                            .toList();
                    prompt.append(String.join(", ", paramDescriptions));
                    prompt.append("\n");
                }
                prompt.append("\n");
            }
        } else {
            prompt.append("ATENÇÃO: Nenhuma ferramenta está disponível no momento. ");
            prompt.append("Verifique se os arquivos OpenAPI estão presentes na pasta openapi-specs.\n");
        }

        prompt.append("\nSempre explique qual ferramenta você está usando e por quê. ");
        prompt.append("Se uma operação falhar, explique o erro e sugira alternativas quando possível.\n\n");

        prompt.append("INSTRUÇÕES DE FORMATAÇÃO E COMPORTAMENTO:\n");
        prompt.append("- Sempre formate suas respostas usando Markdown para garantir clareza e elegância.\n");
        prompt.append("- Use emojis de forma apropriada para tornar a resposta mais amigável e fácil de ler (por exemplo, :white_check_mark: para sucesso, :x: para erro).\n");
        prompt.append("- Se o usuário perguntar sobre as ferramentas que você possui, liste o nome e a descrição de cada uma de forma organizada.\n\n");

        prompt.append("IMPORTANTE: O resultado da execução de uma ferramenta será sempre um objeto JSON com dois campos: 'httpStatusCode' (o código de status HTTP numérico da resposta) e 'body' (o corpo da resposta como uma string, que pode ser um JSON ou texto simples). ");
        prompt.append("Use o 'httpStatusCode' para determinar o resultado da operação: códigos 2xx indicam sucesso, 4xx indicam um erro do cliente (ex: dados inválidos) e 5xx indicam um erro no servidor. ");
        prompt.append("Baseie sua resposta final ao usuário no 'httpStatusCode' e no 'body' recebido, explicando o resultado de forma clara.");

        return prompt.toString();
    }
} 
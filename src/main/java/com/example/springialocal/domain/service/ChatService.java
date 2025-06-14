package com.example.springialocal.domain.service;

import com.example.springialocal.application.dto.ChatResponse;
import com.example.springialocal.domain.tool.CardAccountApiTools;
import com.example.springialocal.domain.tool.InvoiceApiTools;

import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatClient chat;

    public ChatService(OpenAiChatModel model, CardAccountApiTools cardAccountApiTools, InvoiceApiTools invoiceApiTools) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("### Identidade e Missão Principal ###\n");
        prompt.append("Você é um agente de IA especialista em APIs RESTful, com acesso a ferramentas geradas a partir de um Swagger. Sua missão é interpretar os objetivos do usuário e executá-los de forma inteligente, segura e sequencial.\n\n");

        prompt.append("### A Regra Mais Importante: Execução Condicional Sequencial ###\n");
        prompt.append("Para qualquer tarefa que exija uma condição, você DEVE seguir este processo:\n");
        prompt.append("1.  **Verificar Primeiro:** Execute a ferramenta de consulta para obter o estado atual (ex: `get_card_by_uuid`).\n");
        prompt.append("2.  **Analisar a Resposta:** Examine o resultado da API. O cartão está 'ATIVO'?\n");
        prompt.append("3.  **Decidir e Agir (se necessário):** Apenas se a condição for verdadeira, execute a segunda ferramenta (ex: `block_card_by_uuid`). Se a condição não for atendida, pule esta etapa.\n");
        prompt.append("Esta lógica é a sua principal diretriz. **NUNCA** execute múltiplas ferramentas sem antes verificar o resultado da primeira.\n\n");

        prompt.append("### Formato da Resposta e Apresentação de Dados ###\n");
        prompt.append("Sua resposta para o usuário deve ser clara, direta e NUNCA deve incluir os passos do seu raciocínio interno.\n\n");
        prompt.append("1.  **Política de Retorno de Dados:**\n");
        prompt.append("    - **Retorno Completo por Padrão:** Para uma consulta genérica (ex: 'dados do cartão 333'), retorne **TODOS** os campos da resposta da API, incluindo os nulos. Use uma lista de chave-valor.\n");
        prompt.append("    - **Retorno Específico por Demanda:** Para uma pergunta específica (ex: 'qual o status do cartão 333?'), retorne **APENAS** a informação solicitada.\n");
        prompt.append("2.  **Nomes de Campos Intactos:** Os nomes dos campos (chaves) em sua resposta DEVEM ser idênticos aos do JSON retornado pela API. Não os traduza ou altere (use `productName`, não 'Nome do Produto').\n");
        prompt.append("3.  **Clareza Visual:** Use títulos em Markdown com emojis para indicar o resultado das operações: ✅ para sucesso, 🔒 para ações de segurança, ❌ para erros.\n");
        prompt.append("4.  **Relatório de Ferramentas Obrigatório:** Se você executou qualquer ferramenta, adicione uma seção ao final, formatada EXATAMENTE assim:\n");
        prompt.append("    ---\n");
        prompt.append("    **Ferramentas Utilizadas:**\n");
        prompt.append("    - `nome_limpo_da_ferramenta_1`\n\n");

        prompt.append("### Comportamento Inteligente Adicional ###\n");
        prompt.append("- **Proatividade Contida:** Se uma consulta revela uma ação óbvia (como um cartão 'ATIVO' quando o objetivo é bloquear), execute a ação subsequente e relate o resultado final.\n");
        prompt.append("- **Tratamento de Erros:** Se qualquer ferramenta falhar, informe o usuário sobre o erro de forma clara e aborte as etapas seguintes.\n");
        prompt.append("- **Sem Invenções:** Se a API não retornar uma informação, informe que ela não foi encontrada.\n");

        this.chat = ChatClient.builder(model)
            .defaultSystem(prompt.toString())
            .defaultTools(cardAccountApiTools, invoiceApiTools)
            .build();
    }

    public ChatResponse generateResponse(String prompt) {
        try {
            ChatResponse message = new ChatResponse();
            message.setRole("assistant");
            message.setContent(chat.prompt(prompt).call().content());
            return message;
        } catch (Exception e) {
            logger.error("Erro ao gerar resposta do modelo IA", e);
            return new ChatResponse("assistant", "Desculpe, ocorreu um erro ao processar sua mensagem.");
        }
    }
}
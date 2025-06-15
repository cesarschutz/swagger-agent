# Swagger Agent - Transformando APIs em Ferramentas de IA

![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)
![Spring AI](https://img.shields.io/badge/Spring%20AI-Latest-blue.svg)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-yellow.svg)

## 🚀 Visão Geral

O **Swagger Agent** é um projeto educativo e profissional que demonstra como transformar especificações OpenAPI/Swagger em ferramentas inteligentes que agentes de IA podem utilizar. Este sistema permite que desenvolvedores criem rapidamente interfaces conversacionais para suas APIs existentes, democratizando o acesso a sistemas complexos através de linguagem natural.

### 🎯 Objetivo Principal

Facilitar a integração entre APIs REST e agentes de IA, permitindo que usuários interajam com sistemas através de conversas naturais, sem necessidade de conhecimento técnico sobre endpoints, parâmetros ou formatos de dados.

## 🏗️ Arquitetura do Sistema

### Componentes Principais

1. **OpenApiParserService**: Responsável pela leitura e parsing de arquivos swagger.json
2. **DynamicToolGeneratorService**: Gera ferramentas dinâmicas a partir dos endpoints
3. **ChatService**: Gerencia conversas e integração com modelos de IA
4. **ToolLoggerService**: Monitora e registra o uso das ferramentas
5. **SystemPromptService**: Otimiza prompts para múltiplas ferramentas

### Fluxo de Funcionamento

```
Arquivos Swagger → Parsing → Geração de Ferramentas → Integração com IA → Interface de Chat
```

## 🛠️ Tecnologias Utilizadas

### Backend
- **Java 17+**: Linguagem principal
- **Spring Boot 3.x**: Framework base
- **Spring AI**: Integração com modelos de linguagem
- **WebFlux**: Streaming reativo para chat em tempo real
- **Jackson**: Processamento JSON
- **OpenAPI 3.0**: Especificações de API

### Frontend
- **HTML5/CSS3**: Interface moderna e responsiva
- **JavaScript ES6+**: Funcionalidades interativas
- **Server-Sent Events**: Streaming de respostas
- **Marked.js**: Renderização de Markdown

## 📁 Estrutura do Projeto

```
swagger-agent/
├── src/main/java/com/example/swaggeragent/
│   ├── controller/          # Controllers REST
│   ├── service/            # Lógica de negócio
│   ├── model/              # Modelos de dados
│   ├── dto/                # Data Transfer Objects
│   └── config/             # Configurações
├── src/main/resources/
│   ├── static/             # Arquivos estáticos (HTML, CSS, JS)
│   └── application.yml     # Configurações da aplicação
├── openapi-specs/          # Especificações OpenAPI
│   ├── cards-api/
│   ├── invoices-api/
│   └── swagger.json
├── index.html              # Página educativa
└── README.md
```

## 🚀 Como Executar

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6+
- IDE de sua preferência (IntelliJ IDEA, Eclipse, VS Code)

### Passos para Execução

1. **Clone o repositório**
   ```bash
   git clone https://github.com/seu-usuario/swagger-agent.git
   cd swagger-agent
   ```

2. **Configure suas APIs**
   - Adicione seus arquivos `swagger.json` na pasta `openapi-specs/`
   - Organize em subpastas por projeto/microserviço

3. **Execute a aplicação**
   ```bash
   mvn spring-boot:run
   ```

4. **Acesse as interfaces**
   - Página educativa: http://localhost:8080
   - Interface de chat: http://localhost:8080/chat.html

## 🎓 Conceitos Educativos

### O que é Inteligência Artificial?
A IA é a capacidade de máquinas simularem a inteligência humana, processando informações, aprendendo padrões e tomando decisões. No contexto deste projeto, utilizamos modelos de linguagem avançados (LLMs) que podem compreender linguagem natural e executar tarefas complexas.

### O que são Agentes de IA?
Agentes são sistemas de IA autônomos que podem:
- Perceber seu ambiente
- Tomar decisões baseadas em objetivos
- Executar ações através de ferramentas
- Manter memória de interações anteriores
- Raciocinar sobre problemas complexos

### Spring Framework e Spring AI
O Spring é um framework Java maduro para desenvolvimento empresarial. O Spring AI é uma extensão moderna que facilita a integração de capacidades de IA, oferecendo abstrações para modelos de linguagem, embeddings e ferramentas.

### OpenAPI/Swagger
OpenAPI é uma especificação padrão para descrever APIs REST de forma estruturada e legível por máquinas. Isso permite:
- Documentação automática
- Geração de código
- Validação de contratos
- **Criação dinâmica de ferramentas para IA**

## 🔧 Funcionalidades Principais

### ✅ Descoberta Automática de APIs
- Varredura recursiva de diretórios
- Suporte a múltiplos projetos/microserviços
- Detecção automática de mudanças
- Organização hierárquica

### ✅ Geração Inteligente de Ferramentas
- Nomes únicos por projeto/controller
- Descrições contextualizadas
- Validação automática de parâmetros
- Tratamento robusto de erros

### ✅ Interface de Chat Avançada
- Streaming de respostas em tempo real
- Sessões isoladas por usuário
- Memória de conversação
- Painel elegante de ferramentas disponíveis

### ✅ Monitoramento e Logging
- Logs detalhados de uso das ferramentas
- Métricas de performance
- Cache de metadados
- Debugging avançado

## 🎯 Casos de Uso

### Para Desenvolvedores
- **Prototipagem Rápida**: Teste APIs através de conversas naturais
- **Documentação Interativa**: Demonstre funcionalidades para stakeholders
- **Debugging Inteligente**: Investigue problemas através de perguntas
- **Aprendizado**: Entenda como integrar IA em aplicações Java

### Para Usuários de Negócio
- **Acesso Simplificado**: Consulte dados sem conhecimento técnico
- **Relatórios Dinâmicos**: Obtenha informações através de perguntas
- **Automação de Tarefas**: Execute operações complexas com comandos simples
- **Exploração de Dados**: Descubra insights através de conversas

### Para Equipes
- **Democratização de APIs**: Permita que não-desenvolvedores usem sistemas
- **Redução de Suporte**: Usuários podem obter informações autonomamente
- **Inovação**: Explore novos casos de uso para IA conversacional
- **Educação**: Aprenda sobre IA de forma prática

## 🔍 Exemplo Prático

### Especificação OpenAPI Original
```json
{
  "paths": {
    "/cards": {
      "get": {
        "summary": "Lista todos os cartões",
        "operationId": "getAllCards",
        "tags": ["cards-controller"],
        "parameters": [
          {
            "name": "status",
            "in": "query",
            "required": false,
            "schema": { "type": "string" }
          }
        ]
      }
    }
  }
}
```

### Ferramenta Gerada Automaticamente
```
Nome: cards_cards_getAllCards
Descrição: [CARDS API - cards-controller] Lista todos os cartões
Parâmetros: status (string, opcional)
Função: Executa GET /cards com validação automática
```

### Interação com o Usuário
```
Usuário: "Quais cartões ativos temos no sistema?"

IA: Vou consultar os cartões ativos para você.
    [Executa: cards_cards_getAllCards(status: "active")]
    
    Encontrei 15 cartões ativos no sistema:
    - Cartão Premium (ID: 001) - Status: Ativo
    - Cartão Gold (ID: 002) - Status: Ativo
    ...
```

## 🎨 Interface e Experiência

### Página Educativa (index.html)
- Design moderno e responsivo
- Explicações detalhadas de conceitos
- Demonstrações interativas
- Call-to-actions claros

### Interface de Chat (chat.html)
- Streaming de respostas em tempo real
- Painel lateral com ferramentas disponíveis
- Sessões únicas por usuário
- Design profissional e intuitivo

## 🔧 Configuração e Personalização

### Configurações da Aplicação
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.7

swagger-agent:
  specs-directory: openapi-specs
  cache-enabled: true
  max-tools-in-prompt: 50
```

### Adicionando Novas APIs
1. Crie uma subpasta em `openapi-specs/`
2. Adicione seu arquivo `swagger.json`
3. Reinicie a aplicação
4. As ferramentas serão geradas automaticamente

## 🚀 Próximos Passos

### Melhorias Planejadas
- [ ] Suporte a autenticação OAuth2
- [ ] Integração com múltiplos modelos de IA
- [ ] Dashboard de métricas e analytics
- [ ] Suporte a WebSockets para chat em tempo real
- [ ] Testes automatizados abrangentes
- [ ] Documentação de API com Swagger UI

### Contribuições
Contribuições são bem-vindas! Por favor:
1. Faça um fork do projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Abra um Pull Request

## 📚 Recursos Educativos

### Documentação Adicional
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Guia de Agentes de IA](https://example.com/ai-agents-guide)

### Tutoriais Relacionados
- Como criar agentes de IA com Spring
- Integração de LLMs em aplicações Java
- Melhores práticas para APIs REST

## 📄 Licença

Este projeto é licenciado sob a MIT License - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🤝 Suporte e Comunidade

- **Issues**: Reporte bugs ou solicite features
- **Discussions**: Participe de discussões sobre o projeto
- **Wiki**: Documentação adicional e tutoriais
- **Email**: contato@swagger-agent.com

---

**Desenvolvido com ❤️ para democratizar o acesso à Inteligência Artificial**

*Este projeto serve como uma ponte entre o mundo das APIs tradicionais e o futuro da interação humano-computador através de IA conversacional.*


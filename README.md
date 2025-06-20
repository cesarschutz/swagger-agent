# 🤖 Swagger Agent

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-0.8.0-blue.svg)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Documentation](https://img.shields.io/badge/docs-swagger--agent-blue.svg)](https://cesarschutz.github.io/swagger-agent/)

<p align="center">
  <img src="src/main/resources/static/img/logo.png" alt="Swagger Agent Logo" width="200"/>
</p>

<p align="center">
  <strong>Transforme qualquer especificação OpenAPI em ferramentas dinâmicas que podem ser invocadas por modelos de linguagem (LLMs).</strong>
</p>

<p align="center">
  Construído com <strong>Spring Boot 3</strong>, <strong>Spring AI</strong> e preparado para rodar com <strong>OpenAI</strong> ou <strong>modelos locais via Ollama</strong>.
</p>

<p align="center">
  <a href="https://cesarschutz.github.io/swagger-agent/">📚 Documentação Completa</a>
</p>

## 📑 Índice

- [✨ Funcionalidades](#-funcionalidades)
- [🚀 Quick Start](#-quick-start)
- [🏗️ Arquitetura](#️-arquitetura)
- [🧑‍💻 Usando com Modelo Local](#-usando-com-modelo-local-ollama)
- [📚 API Reference](#-api-reference)
- [🔧 Configuração](#-configuração)
- [🤝 Contribuindo](#-contribuindo)
- [📄 Licença](#-licença)

## ✨ Funcionalidades

### 🚀 Chat Inteligente
- Comunicação natural com modelos da OpenAI ou LLMs locais via `/api/chat`
- Memória de sessão para manter contexto entre conversas

### 🖥️ Interface Web Moderna

<div align="center">
  <div style="margin-bottom: 2rem;">
    <h4>🖥️ Desktop</h4>
    <img src="src/main/resources/static/img/interfaceweb.png" alt="Interface Web do Swagger Agent" width="800"/>
  </div>
  
  <div>
    <h4>📱 Mobile</h4>
    <img src="src/main/resources/static/img/interfacewebmobile.png" alt="Interface Mobile do Swagger Agent" width="300"/>
  </div>
</div>

- **🎨 Design Moderno**: Interface com gradientes, efeitos glass e animações suaves
- **🛠️ Painel de Ferramentas**: Visualize todas as APIs disponíveis organizadas por projeto
- **💬 Chat Inteligente**: Conversa natural com suporte a markdown e emojis
- **📱 Responsivo**: Funciona perfeitamente em desktop, tablet e mobile
- **⚡ Tempo Real**: Indicadores de digitação e atualizações instantâneas

### 🛠️ Ferramentas Dinâmicas
- Geração automática de functions a partir de arquivos OpenAPI
- Execução assíncrona para melhor responsividade
- Logs estruturados para auditoria

## 🚀 Quick Start

### 📋 Pré-requisitos

- Java 21 ou superior
- Maven 3.6+
- OpenAI API Key (opcional se usar modelo local)

### ⚡ Instalação

1. Clone o repositório:
```bash
git clone https://github.com/cesarschutz/swagger-agent.git
cd swagger-agent
```

2. Adicione suas especificações OpenAPI:
- Coloque seus arquivos `.json` ou `.yaml` na pasta `openapi-specs/`
- Exemplo incluído: `openapi-specs/petstore/petstore.yaml`

3. Configure a chave da OpenAI (se necessário):
```bash
export OPENAI_API_KEY="sua_chave_openai_aqui"
```

4. Execute a aplicação:
```bash
./mvnw spring-boot:run
```

5. Acesse a interface web:
```
http://localhost:8080/chat.html
```

## 🏗️ Arquitetura

```mermaid
graph TD
    A[Frontend - chat.html] <--> B[Spring Boot Application]
    B <--> C[OpenAI/Ollama]
    B --> D[OpenAPI Specs]
    B <--> E[External APIs]
```

### 📁 Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/example/swaggeragent/
│   │   ├── controller/     # Controllers REST
│   │   ├── service/        # Lógica de Negócio
│   │   ├── model/          # Modelos de Dados
│   │   ├── dto/            # DTOs da API
│   │   └── config/         # Configurações
│   └── resources/
│       ├── static/         # Frontend
│       └── application.yml # Configurações
└── openapi-specs/          # Especificações OpenAPI
```

## 🧑‍💻 Usando com Modelo Local (Ollama)

### 1. Inicie o Ollama
```bash
docker run -d --name ollama -p 11434:11434 ollama/ollama:latest
```

### 2. Baixe um modelo
```bash
docker exec -it ollama ollama pull qwen:0.5b
```

### 3. Configure o ambiente
```bash
export SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434
export SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL="qwen:0.5b"
```

## 📚 API Reference

### Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/chat` | Chat síncrono |
| `POST` | `/api/chat/stream` | Chat com streaming |
| `GET`  | `/api/tools` | Lista ferramentas |

### Exemplo de Uso

```bash
# Chat síncrono
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Quais pets estão disponíveis?",
    "sessionId": "sessao-123"
  }'
```

## 🔧 Configuração

### Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `OPENAI_API_KEY` | Chave da API OpenAI | - |
| `SPRING_AI_OLLAMA_BASE_URL` | URL base do Ollama | `http://localhost:11434` |
| `SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL` | Modelo do Ollama | `qwen:0.5b` |

### application.yml
```yaml
spring:
  ai:
    ollama:
      base-url: ${SPRING_AI_OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: ${SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL:qwen:0.5b}
```

## 🤝 Contribuindo

1. Faça um Fork do projeto
2. Crie sua Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a Branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

### 🔗 Links Úteis

- [📚 Documentação](https://cesarschutz.github.io/swagger-agent/)
- [💻 Código Fonte](https://github.com/cesarschutz/swagger-agent)
- [🎮 Interface Demo](https://cesarschutz.github.io/swagger-agent/demo-chat.html)

# 🤖 Exemplo de Configuração de Provedores de IA

Este arquivo demonstra como configurar e usar diferentes provedores de IA no Swagger Agent.

## 🔧 Configuração Básica

O Swagger Agent suporta múltiplos provedores de IA através da propriedade `app.ai.provider`. Apenas **um provedor será carregado por vez**.

### Propriedade de Configuração

```yaml
# application.yml
app:
  ai:
    provider: ${AI_PROVIDER:openai}  # Valores: "openai" ou "ollama"
```

## 🚀 Exemplo 1: Usando OpenAI

### Configuração via Variável de Ambiente

```bash
# Configure o provedor como OpenAI
export AI_PROVIDER=openai

# Configure sua chave da API OpenAI
export OPENAI_API_KEY="sua_chave_openai_aqui"

# Execute a aplicação
mvn spring-boot:run
```

### Configuração via application.yml

```yaml
# application.yml
app:
  ai:
    provider: openai

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
          max-tokens: 2048
```

## 🏠 Exemplo 2: Usando Ollama

### Pré-requisitos

1. **Instale o Ollama:**
```bash
# Com Docker
docker run -d --name ollama -p 11434:11434 ollama/ollama:latest

# Ou instale diretamente: https://ollama.com/download
```

2. **Baixe um modelo:**
```bash
# Exemplo com Qwen 2.5
docker exec -it ollama ollama pull qwen2.5:0.5b

# Ou se instalou diretamente
ollama pull qwen2.5:0.5b
```

### Configuração via Variável de Ambiente

```bash
# Configure o provedor como Ollama
export AI_PROVIDER=ollama

# Configure o Ollama
export SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434
export SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL="qwen2.5:0.5b"

# Execute a aplicação
mvn spring-boot:run
```

### Configuração via application.yml

```yaml
# application.yml
app:
  ai:
    provider: ollama

spring:
  ai:
    ollama:
      base-url: ${SPRING_AI_OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: ${SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL:qwen2.5:0.5b}
          temperature: 0.7
```

## 🔄 Exemplo 3: Troca de Provedores

### De OpenAI para Ollama

```bash
# 1. Pare a aplicação (Ctrl+C)

# 2. Configure para usar Ollama
export AI_PROVIDER=ollama
export SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434
export SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL="qwen2.5:0.5b"

# 3. Reinicie a aplicação
mvn spring-boot:run
```

### De Ollama para OpenAI

```bash
# 1. Pare a aplicação (Ctrl+C)

# 2. Configure para usar OpenAI
export AI_PROVIDER=openai
export OPENAI_API_KEY="sua_chave_openai_aqui"

# 3. Reinicie a aplicação
mvn spring-boot:run
```

## 📋 Logs de Inicialização

### Log com OpenAI
```
🔧 Configurando OpenAI como provedor de IA
```

### Log com Ollama
```
🔧 Configurando Ollama como provedor de IA
```

## ⚠️ Importante

- **Apenas um provedor é carregado por vez**
- **A aplicação deve ser reiniciada após alterar o provedor**
- **Verifique os logs para confirmar qual provedor está sendo usado**

## 🧪 Testando

Após configurar, teste a aplicação:

1. **Acesse a interface web:** `http://localhost:8080/chat.html`
2. **Faça uma pergunta:** "Olá, como você está?"
3. **Verifique se está funcionando** com o provedor configurado

## 🔍 Troubleshooting

### Erro: "No qualifying bean of type 'ChatModel'"
- Verifique se a propriedade `app.ai.provider` está configurada corretamente
- Certifique-se de que as dependências do provedor estão no classpath

### Erro: "Connection refused" (Ollama)
- Verifique se o Ollama está rodando: `docker ps` ou `ollama list`
- Confirme a URL: `http://localhost:11434`

### Erro: "Invalid API key" (OpenAI)
- Verifique se a variável `OPENAI_API_KEY` está configurada
- Confirme se a chave é válida 
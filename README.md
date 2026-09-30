# DIO Budgeting Gemini

API inteligente de controle financeiro desenvolvida com **Java, Spring Boot, Spring AI, Google Gemini e PostgreSQL**.

O projeto faz parte de um desafio da DIO sobre construção de uma API inteligente com reconhecimento de fala. A implementação original do curso utiliza outros provedores em alguns pontos; neste projeto, o fluxo foi adaptado para utilizar **Google Gemini** e **PostgreSQL**.

## Objetivo

Construir um assistente financeiro capaz de receber comandos por áudio, interpretar a intenção do usuário, executar ações reais da aplicação e responder em voz. O projeto também mantém endpoints de estudo para conversar diretamente com o modelo por texto.

O fluxo principal implementado é:

```text
Áudio
  ↓
Gemini - transcrição
  ↓
ChatClient + System Prompt
  ↓
Tool Calling
  ├── persist-transaction
  └── list-transactions-by-category
  ↓
PostgreSQL
  ↓
Resposta textual do Gemini
  ↓
Gemini Text-to-Speech
  ↓
Áudio WAV
```

## Tecnologias

- Java 25
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Google Gemini
- Gemini 2.5 Flash
- Gemini Text-to-Speech
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL 14
- Docker Compose
- Gradle 9.6.1
- JUnit 5
- WSL2
- IntelliJ IDEA

## Integração com o Gemini

A aplicação utiliza Spring AI para o modelo de chat:

```properties
spring.ai.model.chat=google-genai
spring.ai.google.genai.api-key=${GOOGLE_API_KEY}
spring.ai.google.genai.chat.model=gemini-2.5-flash
```

A chave da API deve ser fornecida pela variável de ambiente `GOOGLE_API_KEY` e nunca versionada.

## ChatModel e ChatClient

Endpoints de estudo mantidos no projeto:

```http
GET /api/chat-model
GET /api/chat
```

Exemplo:

```bash
curl --get \
  --data-urlencode "prompt=Olá, quem é você?" \
  http://localhost:8080/api/chat-model
```

## Tool Calling

Os casos de uso de transações foram expostos ao Spring AI como tools:

```text
persist-transaction
list-transactions-by-category
```

`PersistTransactionInput` utiliza `@ToolParam` para dar contexto ao modelo sobre:

- descrição do gasto;
- valor em centavos;
- categoria da transação.

## Transcrição de áudio

A transcrição usa o Gemini multimodal por meio do `ChatModel`. O serviço `GeminiAudioTranscriptionService` é chamado no fluxo de `POST /transactions/ai`. Não há endpoint HTTP exclusivo para transcrição.

Serviço:

```text
GeminiAudioTranscriptionService
```

## Text-to-Speech

A síntese de voz utiliza uma integração direta com a API Gemini.

Configuração:

```properties
app.gemini.tts.model=gemini-3.8-flash-lite-tts
app.gemini.tts.voice=Kore
```

Serviço:

```text
GeminiTextToSpeechService
```

Endpoint dedicado:

```http
POST /api/synthesize
```

A saída é retornada em formato WAV. Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/api/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"Seu gasto foi registrado."}' \
  -o speech.wav
```

## Domínio de transações

Uma transação possui:

```text
id
description
amount
category
```

`amount` é recebido e armazenado em **centavos**. Na resposta da API REST, o campo `amount` é apresentado em reais.

Exemplos:

```text
R$ 80,00   → 8000
R$ 125,33  → 12533
```

Categorias atuais:

```text
GROCERIES
PHARMACY
OTHER
```

O identificador é representado por `TransactionId` e utiliza UUID.

O domínio só aceita transações com descrição não vazia de até 255 caracteres, valor maior que zero e categoria definida. Essas regras também são aplicadas quando o Gemini chama a ferramenta de persistência.

## Persistência

A aplicação utiliza PostgreSQL com Spring Data JPA.

Principais componentes:

```text
TransactionEntity
TransactionEntityRepository
JpaTransactionRepository
TransactionRepository
```

A interface `TransactionRepository` permanece no domínio, enquanto a implementação JPA fica na infraestrutura.

O Hibernate está configurado para preservar schema e dados entre reinicializações:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## PostgreSQL com Docker Compose

Configuração de desenvolvimento em `compose.yml`:

```yaml
services:
  database:
    image: postgres:14
    environment:
      POSTGRES_DB: transaction
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports:
      - "5433:5432"
```

Iniciar o banco:

```bash
docker compose up -d database
```

Verificar container:

```bash
docker compose ps
```

Consultar transações:

```bash
docker compose exec database psql -U app -d transaction \
  -c "SELECT id, description, amount, category FROM transaction_entity;"
```

## API de transações

### Criar transação

```http
POST /transactions
Content-Type: application/json
```

```bash
curl -i -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Compras no mercado",
    "category": "GROCERIES",
    "amount": 12533
  }'
```

Resposta esperada:

```json
{
  "id": "uuid",
  "category": "GROCERIES",
  "description": "Compras no mercado",
  "amount": 125.33
}
```

Status:

```text
201 Created
```

Para criar uma transação, `description` deve conter texto e ter no máximo 255 caracteres, `amount` deve ser maior que zero (em centavos), e `category` deve ser uma das categorias disponíveis. As mesmas regras são aplicadas às transações registradas pelo assistente de IA. Um valor ausente ou inválido recebe `400 Bad Request`.

Uma requisição com `amount: 0`, por exemplo, recebe `400 Bad Request`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Dados da transação inválidos",
  "errors": [
    { "field": "amount", "message": "deve ser maior que zero" }
  ]
}
```

### Listar por categoria

```http
GET /transactions/{category}
```

```bash
curl http://localhost:8080/transactions/GROCERIES
```

### Processar transação por áudio com IA

```http
POST /transactions/ai
Content-Type: multipart/form-data
```

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/Recording1.m4a" \
  -o response.wav
```

Fluxo:

```text
MultipartFile
    ↓
GeminiAudioTranscriptionService
    ↓
texto transcrito
    ↓
ChatClient
    ↓
Gemini identifica a intenção
    ↓
Tool Calling
    ↓
PersistTransactionUseCase
ou
ListTransactionsByCategoryUseCase
    ↓
PostgreSQL
    ↓
resposta textual
    ↓
GeminiTextToSpeechService
    ↓
response.wav
```

Exemplo validado durante o desenvolvimento:

```text
Áudio:
"Eu passei na farmácia, comprei três itens e gastei R$ 80."

Resultado persistido:
description = compra de três itens
amount = 8000
category = PHARMACY
```

## System Prompt

O contexto do assistente financeiro fica em:

```text
src/main/resources/prompts/system-message.st
```

Ele orienta o modelo a extrair dados de transações, utilizar as tools disponíveis e escolher a categoria adequada ao contexto.

## Estrutura do projeto

```text
src/main/java/io/github/peuvictor/budgeting/
├── application/
│   ├── input/ e output/               # dados dos casos de uso
│   ├── port/                           # contratos de áudio e assistente
│   ├── PersistTransactionUseCase.java
│   ├── ListTransactionsByCategoryUseCase.java
│   └── ProcessAudioTransactionUseCase.java
├── domain/                             # transação, categorias e validação
├── infrastructure/
│   ├── http/                           # endpoints, DTOs e erros de validação
│   ├── ia/                             # transcrição e assistente Spring AI
│   └── persistence/                    # entidade e repositórios JPA
├── ChatModelController.java
├── ChatClientController.java
├── TextToSpeechController.java
└── GeminiTextToSpeechService.java

src/main/resources/
├── application.properties
└── prompts/system-message.st

src/test/java/io/github/peuvictor/budgeting/
├── application/ e domain/             # testes locais das regras
├── infrastructure/http/               # testes locais do endpoint REST
└── *IT.java                            # integrações com Gemini
```

## Como executar

### 1. Pré-requisitos

```text
Java 25
Docker / Docker Desktop
```

### 2. Configure a API Key

No `zsh`:

```bash
read -s "GOOGLE_API_KEY?Google API Key: "
echo
export GOOGLE_API_KEY
```

### 3. Inicie o PostgreSQL

```bash
docker compose up -d database
```

### 4. Compile

```bash
./gradlew compileJava
```

### 5. Execute

```bash
./gradlew bootRun
```

API:

```text
http://localhost:8080
```

PostgreSQL no host:

```text
localhost:5433
```

## Testes

Os testes locais verificam as regras do domínio, a rejeição de entradas inválidas antes da persistência e as respostas HTTP de validação. Eles não chamam o Gemini nem precisam de PostgreSQL:

```bash
./gradlew test \
  --tests 'io.github.peuvictor.budgeting.domain.TransactionTest' \
  --tests 'io.github.peuvictor.budgeting.application.PersistTransactionUseCaseTest' \
  --tests 'io.github.peuvictor.budgeting.infrastructure.http.TransactionControllerValidationTest'
```

O projeto também possui testes de integração para:

- `ChatModel`;
- `ChatClient`;
- Tool Calling;
- transcrição de áudio com Gemini;
- Text-to-Speech com Gemini;
- carregamento do contexto Spring.

Para executar a suíte completa, configure `GOOGLE_API_KEY`, inicie o PostgreSQL e execute:

```bash
./gradlew test
```

Os testes de integração fazem chamadas reais ao Gemini; podem consumir quota e estão sujeitos a indisponibilidades temporárias do provedor. Sem `GOOGLE_API_KEY`, a suíte completa não consegue carregar o contexto da aplicação.

## Arquitetura

```text
Domain
   ↓
Application
   ↓
Infrastructure
```

**Domain:** regras e conceitos centrais do negócio.

**Application:** casos de uso e orquestração das operações.

**Infrastructure:** API REST, PostgreSQL/JPA, Gemini, transcrição e síntese de voz.

## Status

```text
Spring Boot                         ✅
Spring AI                           ✅
Google Gemini                       ✅
ChatModel                           ✅
ChatClient                          ✅
Tool Calling                        ✅
Domínio de transações               ✅
PostgreSQL                          ✅
Spring Data JPA                     ✅
Docker Compose                      ✅
POST /transactions                  ✅
Validação de transações             ✅
GET /transactions/{category}        ✅
Transcrição de áudio                ✅
Text-to-Speech                      ✅
POST /transactions/ai               ✅
Resposta em áudio WAV               ✅
```

## Aprendizados

- Spring Boot e Gradle;
- Spring AI;
- Google Gemini;
- `ChatModel` e `ChatClient`;
- System Prompt;
- Tool Calling;
- `@Tool` e `@ToolParam`;
- arquitetura em camadas;
- casos de uso e value objects;
- Spring Data JPA;
- PostgreSQL;
- Docker Compose;
- endpoints REST;
- validação de dados com Bean Validation;
- multipart/form-data;
- processamento multimodal de áudio;
- transcrição de voz;
- Text-to-Speech;
- testes de integração;
- Git e GitHub.

## Observações

O projeto utiliza serviços externos do Gemini. Chamadas de integração podem eventualmente falhar por quota, alta demanda ou respostas transitórias do provedor.

Nunca armazene chaves de API diretamente no código ou em arquivos versionados.

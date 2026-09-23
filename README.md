# DIO Budgeting Gemini

API de controle financeiro desenvolvida com Java, Spring Boot e Spring AI, utilizando o Google Gemini como modelo de inteligência artificial.

O projeto faz parte de um desafio da DIO sobre construção de uma API inteligente para gerenciamento de orçamento pessoal.

> 🚧 **Projeto em desenvolvimento.**

## Sumário

- [Objetivo](#objetivo)
- [Tecnologias](#tecnologias)
- [Integração com o Gemini](#integração-com-o-gemini)
- [ChatModel](#chatmodel)
- [ChatClient](#chatclient)
- [Estrutura atual](#estrutura-atual)
- [Como executar](#como-executar)
- [Testes](#testes)
- [Próximas etapas](#próximas-etapas)
- [Aprendizados até aqui](#aprendizados-até-aqui)
- [Status](#status)

## Objetivo

A proposta final do projeto é criar um assistente financeiro capaz de receber comandos do usuário, interpretar sua intenção utilizando inteligência artificial e executar ações reais da aplicação.

O fluxo completo do desafio deverá evoluir para:

1. Receber um comando do usuário;
2. Processar e interpretar esse comando com IA;
3. Identificar a intenção;
4. Executar funções da aplicação;
5. Registrar ou consultar transações financeiras;
6. Gerar uma resposta para o usuário.

Nesta etapa do projeto, a integração de texto com o Gemini já está funcionando por meio do `ChatModel` e do `ChatClient`.

## Tecnologias

- Java 25
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Google Gemini
- Gemini 2.5 Flash
- Spring Web
- Gradle 9.6.1
- JUnit 5
- WSL2
- IntelliJ IDEA

## Integração com o Gemini

A aplicação utiliza o Spring AI para abstrair a comunicação com o modelo de linguagem.

A implementação atual utiliza:

```properties
spring.ai.google.genai.api-key=${GOOGLE_API_KEY}
spring.ai.google.genai.chat.model=gemini-2.5-flash
```

A chave da API não é armazenada no projeto.

Ela deve ser fornecida através da variável de ambiente:

`GOOGLE_API_KEY`

> **Atenção:** Nunca adicione sua API Key diretamente ao código ou ao repositório Git.

## ChatModel

O `ChatModel` representa a integração de baixo nível entre a aplicação e o modelo de linguagem.

O projeto possui um teste de integração que envia um prompt diretamente ao Gemini:

```java
String response = chatModel.call(
    "Gere um registro de gasto com descrição, valor em reais e local."
);
```

O teste verifica se a resposta:

- não é nula;
- não está vazia.

Também foi criado um endpoint REST utilizando o `ChatModel`.

### Endpoint

`GET /api/chat-model`

**Parâmetro:** `prompt`

Exemplo:

```bash
curl --get \
  --data-urlencode "prompt=Olá, quem é você?" \
  http://localhost:8080/api/chat-model
```

**Fluxo:**

```text
HTTP Request
↓
ChatModelController
↓
ChatModel
↓
Google Gemini
↓
Resposta
```

## ChatClient

O Spring AI também disponibiliza o `ChatClient`, uma API de nível mais alto construída sobre o `ChatModel`.

Ele permite estruturar melhor a interação com o modelo, separando, por exemplo:

- **System Prompt**
- **User Prompt**

No teste de integração atual, o sistema define o modelo como matemático:

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultSystem("Você é um matemático.")
    .build();
```

Depois envia uma instrução do usuário:

```java
String response = chatClient.prompt()
    .user(
        "Some 10 + 20. Depois subtraia 30 do resultado anterior. " +
        "Exiba apenas o resultado final sem explicações."
    )
    .call()
    .content();
```

O resultado esperado contém: `0`.

### Bean do ChatClient

Para permitir a injeção de dependência do `ChatClient` nos componentes da aplicação, foi criado um bean:

```java
@Bean
ChatClient chatClient(ChatClient.Builder builder) {
    return builder.build();
}
```

O Spring AI fornece o `ChatClient.Builder` através da autoconfiguração e a aplicação utiliza esse builder para criar uma instância do `ChatClient`.

### Endpoint com ChatClient

Também foi criado um endpoint utilizando o `ChatClient`.

`GET /api/chat`

Exemplo:

```bash
curl --get \
  --data-urlencode "prompt=Bom dia" \
  http://localhost:8080/api/chat
```

**Fluxo:**

```text
HTTP Request
↓
ChatClientController
↓
ChatClient
↓
ChatModel
↓
Google Gemini
↓
Resposta
```

## Estrutura atual

```text
src
├── main
│   ├── java/io/github/peuvictor/budgeting
│   │   ├── DioBudgetingGeminiApplication.java
│   │   ├── ChatModelController.java
│   │   └── ChatClientController.java
│   └── resources
│       └── application.properties
└── test
    └── java/io/github/peuvictor/budgeting
        ├── DioBudgetingGeminiApplicationTests.java
        ├── GoogleGenAiChatModelIT.java
        └── GoogleGenAiChatClientIT.java
```

## Como executar

### 1. Clone o projeto

Clone o repositório e entre na pasta do projeto.

### 2. Configure a API Key

Crie uma chave para a Gemini API e disponibilize-a através da variável:

`GOOGLE_API_KEY`

No zsh, por exemplo:

```zsh
read -s "GOOGLE_API_KEY?Google API Key: "
echo
export GOOGLE_API_KEY
```

### 3. Execute os testes

```bash
./gradlew test
```

### 4. Execute a aplicação

```bash
./gradlew bootRun
```

A aplicação será iniciada por padrão em:

[http://localhost:8080](http://localhost:8080)

## Testes

Atualmente existem testes para:

- carregamento do contexto Spring;
- integração direta com `ChatModel`;
- integração utilizando `ChatClient`;
- chamada real ao Google Gemini.

Para executar todos:

```bash
./gradlew test
```

## Próximas etapas

O projeto ainda será evoluído com os recursos apresentados nas próximas aulas do desafio.

Entre as próximas implementações estão:

- Tool Calling;
- execução de funções Java a partir da intenção identificada pela IA;
- domínio de transações financeiras;
- persistência de dados;
- consultas e relatórios financeiros;
- processamento de áudio;
- transcrição de voz para texto;
- geração de respostas em áudio;
- novos testes;
- documentação dos endpoints.

## Aprendizados até aqui

Até esta etapa, o projeto permitiu praticar conceitos como:

- configuração de um projeto Spring Boot;
- gerenciamento de dependências com Gradle;
- Spring AI;
- integração com Google Gemini;
- variáveis de ambiente;
- autoconfiguração do Spring;
- injeção de dependência;
- `ChatModel`;
- `ChatClient`;
- System Prompt e User Prompt;
- criação de Beans;
- endpoints REST;
- testes de integração;
- versionamento com Git e GitHub.

## Status

| Recurso | Status |
| --- | --- |
| Spring Boot | ✅ Concluído |
| Spring AI | ✅ Concluído |
| Google Gemini | ✅ Concluído |
| ChatModel | ✅ Concluído |
| ChatModel REST API | ✅ Concluído |
| ChatClient | ✅ Concluído |
| ChatClient REST API | ✅ Concluído |
| Testes de integração | ✅ Concluído |
| Tool Calling | ⏳ Pendente |
| Persistência | ⏳ Pendente |
| Processamento de áudio | ⏳ Pendente |

O projeto continuará sendo evoluído ao longo das próximas etapas do desafio.

# DIO Budgeting Gemini

API de controle financeiro desenvolvida com Java, Spring Boot e Spring AI, utilizando o Google Gemini como modelo de inteligência artificial.

O projeto faz parte de um desafio da DIO sobre construção de uma API inteligente para gerenciamento de orçamento pessoal.

Durante o desenvolvimento, algumas implementações originalmente apresentadas no curso com OpenAI foram adaptadas para utilizar o ecossistema do Google Gemini.

> 🚧 **Projeto em desenvolvimento.**

## Sumário

- [Objetivo](#objetivo)
- [Tecnologias](#tecnologias)
- [Integração com o Gemini](#integração-com-o-gemini)
- [ChatModel](#chatmodel)
- [ChatClient](#chatclient)
- [Tool Calling](#tool-calling)
- [Transcrição de áudio](#transcrição-de-áudio)
- [Text-to-Speech](#text-to-speech)
- [Endpoints](#endpoints)
- [Estrutura atual](#estrutura-atual)
- [Como executar](#como-executar)
- [Testes](#testes)
- [Próximas etapas](#próximas-etapas)
- [Aprendizados até aqui](#aprendizados-até-aqui)
- [Status](#status)

## Objetivo

A proposta final do projeto é criar um assistente financeiro capaz de receber comandos do usuário, interpretar sua intenção utilizando inteligência artificial e executar ações reais da aplicação.

O fluxo esperado da aplicação é:

1. Receber um comando do usuário;
2. Processar e interpretar esse comando com IA;
3. Identificar a intenção;
4. Executar funções Java através de Tool Calling;
5. Registrar ou consultar informações financeiras;
6. Receber comandos em áudio;
7. Converter áudio em texto;
8. Gerar respostas em texto ou áudio.

Atualmente o projeto já possui integração com Gemini para texto, Tool Calling, transcrição de áudio e geração de voz.

## Tecnologias

- Java 25
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Google Gemini
- Gemini 2.5 Flash
- Gemini 3.8 Flash-Lite TTS
- Spring Web
- Gradle 9.6.1
- JUnit
- AssertJ
- WSL2
- IntelliJ IDEA

## Integração com o Gemini

A aplicação utiliza o Spring AI para a integração principal com os modelos Gemini.

A configuração de chat utiliza:

```properties
spring.ai.model.chat=google-genai
spring.ai.google.genai.api-key=${GOOGLE_API_KEY}
spring.ai.google.genai.chat.model=gemini-2.5-flash
```

Para Text-to-Speech:

```properties
app.gemini.tts.model=gemini-3.8-flash-lite-tts
app.gemini.tts.voice=Kore
```

A chave da API não é armazenada no projeto.

Ela deve ser fornecida através da variável de ambiente:

```text
GOOGLE_API_KEY
```

> **Atenção:** nunca adicione sua API Key diretamente ao código ou ao repositório Git.

---

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

Também foi criado um endpoint REST utilizando diretamente o `ChatModel`.

### Endpoint

```text
GET /api/chat-model
```

Parâmetro:

```text
prompt
```

Exemplo:

```bash
curl --get \
  --data-urlencode "prompt=Olá, quem é você?" \
  http://localhost:8080/api/chat-model
```

Fluxo:

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

---

## ChatClient

O Spring AI também disponibiliza o `ChatClient`, uma API de nível mais alto construída sobre o `ChatModel`.

Ele permite estruturar melhor a comunicação com o modelo utilizando recursos como:

- System Prompt;
- User Prompt;
- Tool Calling;
- configurações padrão de interação.

Exemplo utilizado no teste:

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultSystem("Você é um matemático.")
    .build();

String response = chatClient.prompt()
    .user(
        "Some 10 + 20. Depois subtraia 30 do resultado anterior. " +
        "Exiba apenas o resultado final sem explicações."
    )
    .call()
    .content();
```

O resultado esperado contém:

```text
0
```

### Bean do ChatClient

Para permitir a injeção do `ChatClient` nos componentes da aplicação, foi criado um Bean:

```java
@Bean
ChatClient chatClient(ChatClient.Builder builder) {
    return builder.build();
}
```

### Endpoint

```text
GET /api/chat
```

Exemplo:

```bash
curl --get \
  --data-urlencode "prompt=Bom dia" \
  http://localhost:8080/api/chat
```

Fluxo:

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

---

## Tool Calling

O projeto também implementa Tool Calling através do Spring AI.

Tools permitem que o modelo solicite a execução de métodos Java durante a conversa.

Foi criada uma classe de ferramentas matemáticas:

```java
static class MathTools {

    @Tool(description = "Soma dois números inteiros, a e b")
    public int sum(int a, int b) {
        return a + b;
    }

    @Tool(description = "Subtrai dois números inteiros, a e b")
    public int diff(int a, int b) {
        return a - b;
    }
}
```

As ferramentas são registradas no `ChatClient`:

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultSystem("Você é um matemático.")
    .defaultTools(new MathTools())
    .build();
```

Quando o usuário solicita:

```text
Some 10 mais 20.
Depois subtraia 30 do resultado anterior.
```

o Gemini identifica quais funções precisam ser utilizadas.

O fluxo é semelhante a:

```text
Prompt
  ↓
Gemini
  ↓
sum(10, 20)
  ↓
30
  ↓
diff(30, 30)
  ↓
0
  ↓
Resposta final
```

Essa funcionalidade será importante posteriormente para permitir que o assistente financeiro execute ações reais, como registrar gastos ou consultar transações.

---

## Transcrição de áudio

A aula original do curso utiliza a abstração de transcrição de áudio integrada ao provedor OpenAI.

Neste projeto, a implementação foi adaptada para Gemini.

Em vez de utilizar um `TranscriptionModel`, o áudio é enviado diretamente ao Gemini como conteúdo multimodal utilizando o `ChatModel`.

O áudio é carregado como `Media`:

```java
new Media(
    MimeTypeUtils.parseMimeType("audio/m4a"),
    audio
)
```

e enviado junto a um `UserMessage`:

```java
var userMessage = UserMessage.builder()
    .text("""
        Transcreva este áudio em português brasileiro.
        Retorne apenas o texto falado, sem explicações adicionais.
        """)
    .media(List.of(
        new Media(
            MimeTypeUtils.parseMimeType("audio/m4a"),
            audio
        )
    ))
    .build();
```

Depois o conteúdo é enviado ao modelo:

```java
ChatResponse response = chatModel.call(
    new Prompt(userMessage)
);
```

Fluxo:

```text
Arquivo .m4a
     ↓
Media
     ↓
UserMessage
     ↓
Prompt
     ↓
ChatModel
     ↓
Google Gemini
     ↓
Texto transcrito
```

O teste de integração foi validado com áudio real.

Exemplo de transcrição retornada:

```text
Eu passei na farmácia, comprei três itens e gastei R$ 80.
```

---

## Text-to-Speech

Também foi implementada geração de áudio a partir de texto.

A aula original utiliza uma abstração de Speech Model com OpenAI.

Neste projeto, o recurso foi adaptado para Gemini utilizando diretamente a API de Text-to-Speech do Google através do `RestClient`.

O modelo utilizado é:

```properties
app.gemini.tts.model=gemini-3.8-flash-lite-tts
```

A voz configurada é:

```properties
app.gemini.tts.voice=Kore
```

O modelo Flash-Lite foi escolhido por atender bem ao cenário do projeto e ter foco em menor custo e baixa latência.

Foi criado o serviço:

```text
GeminiTextToSpeechService
```

Sua responsabilidade é receber texto:

```java
byte[] audio = textToSpeechService.synthesize(text);
```

e retornar os bytes do áudio produzido pelo Gemini.

Fluxo:

```text
String
  ↓
GeminiTextToSpeechService
  ↓
Gemini TTS API
  ↓
Áudio em Base64
  ↓
byte[]
  ↓
WAV
```

O teste de integração gera um arquivo WAV temporário e valida que o áudio possui conteúdo.

Um dos arquivos gerados apresentou:

```text
RIFF WAVE
Microsoft PCM
16 bit
mono
24000 Hz
```

---

## Endpoint Text-to-Speech

Foi criado também um endpoint REST para geração de áudio.

```text
POST /api/synthesize
```

Corpo da requisição:

```json
{
  "text": "O valor total ficou em 80 reais. Posso confirmar o pagamento?"
}
```

Exemplo com `curl`:

```bash
curl -X POST http://localhost:8080/api/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"O valor total ficou em 80 reais. Posso confirmar o pagamento?"}' \
  --output speech.wav
```

A resposta possui:

```text
Content-Type: audio/wav
```

e pode ser salva diretamente como arquivo WAV.

Fluxo completo:

```text
POST /api/synthesize
        ↓
TextToSpeechRequest
        ↓
TextToSpeechController
        ↓
GeminiTextToSpeechService
        ↓
Gemini 3.8 Flash-Lite TTS
        ↓
byte[]
        ↓
audio/wav
```

Durante os testes também foi observado que o serviço Gemini TTS pode eventualmente responder com `503 Service Unavailable` em momentos de alta demanda. Uma nova tentativa posteriormente retornou o áudio normalmente.

---

## Endpoints

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/api/chat-model` | Envia um prompt diretamente ao `ChatModel` |
| GET | `/api/chat` | Envia um prompt utilizando `ChatClient` |
| POST | `/api/synthesize` | Converte texto em áudio WAV utilizando Gemini TTS |

---

## Estrutura atual

```text
src
├── main
│   ├── java/io/github/peuvictor/budgeting
│   │   ├── DioBudgetingGeminiApplication.java
│   │   ├── ChatModelController.java
│   │   ├── ChatClientController.java
│   │   ├── GeminiTextToSpeechService.java
│   │   ├── TextToSpeechController.java
│   │   └── TextToSpeechRequest.java
│   │
│   └── resources
│       └── application.properties
│
└── test
    ├── java/io/github/peuvictor/budgeting
    │   ├── DioBudgetingGeminiApplicationTests.java
    │   ├── GoogleGenAiChatModelIT.java
    │   ├── GoogleGenAiChatClientIT.java
    │   ├── ToolCallingIT.java
    │   ├── GeminiAudioTranscriptionIT.java
    │   └── GeminiTextToSpeechIT.java
    │
    └── resources
        └── audio
            ├── Recording1.m4a
            ├── Recording2.m4a
            └── Recording3.m4a
```

---

## Como executar

### 1. Clone o projeto

Clone o repositório e entre na pasta:

```bash
git clone git@github.com:Peuvictor/dio-budgeting-gemini.git
cd dio-budgeting-gemini
```

### 2. Configure a API Key

Crie uma chave da Gemini API e disponibilize através da variável:

```text
GOOGLE_API_KEY
```

No zsh:

```zsh
read -s "GOOGLE_API_KEY?Google API Key: "
echo
export GOOGLE_API_KEY
```

Para confirmar que a variável está disponível sem exibir a chave:

```bash
if [ -n "$GOOGLE_API_KEY" ]; then
  echo "GOOGLE_API_KEY configurada"
else
  echo "GOOGLE_API_KEY NÃO configurada"
fi
```

### 3. Execute a aplicação

```bash
./gradlew bootRun
```

A aplicação será iniciada em:

```text
http://localhost:8080
```

### 4. Execute os testes

```bash
./gradlew test
```

Também é possível executar testes individualmente.

Exemplo:

```bash
./gradlew test \
  --tests "io.github.peuvictor.budgeting.GeminiTextToSpeechIT" \
  --no-daemon
```

> Os testes de integração realizam chamadas reais à Gemini API e, portanto, dependem da disponibilidade do serviço e das cotas da conta.

---

## Testes

Atualmente existem testes para:

- carregamento do contexto Spring;
- integração direta com `ChatModel`;
- integração utilizando `ChatClient`;
- Tool Calling;
- execução de ferramentas Java;
- transcrição de áudio com Gemini;
- geração de áudio com Gemini TTS;
- chamadas reais à Gemini API.

Os testes que utilizam serviços externos podem falhar caso:

- a API Key não esteja configurada;
- a cota da Gemini API tenha sido atingida;
- o modelo esteja temporariamente indisponível;
- o serviço esteja enfrentando alta demanda.

---

## Próximas etapas

O projeto continuará evoluindo com os próximos recursos do desafio.

Entre as próximas implementações estão:

- criação do domínio financeiro;
- modelagem de transações;
- registro de receitas e despesas;
- persistência de dados;
- integração entre Tool Calling e regras da aplicação;
- consultas financeiras;
- relatórios;
- tratamento global de erros;
- melhorias nos endpoints;
- novos testes;
- evolução da documentação.

---

## Aprendizados até aqui

Até esta etapa, o projeto permitiu praticar:

- configuração de projetos Spring Boot;
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
- Tool Calling;
- anotação `@Tool`;
- chamadas de funções Java controladas pelo modelo;
- conteúdo multimodal;
- envio de áudio para modelos Gemini;
- transcrição de áudio;
- integração REST com serviços externos;
- `RestClient`;
- conversão de Base64 para `byte[]`;
- geração de arquivos WAV;
- criação de endpoints REST;
- `ResponseEntity`;
- testes de integração;
- diagnóstico de erros HTTP;
- gerenciamento de cotas e indisponibilidade de APIs;
- versionamento com Git e GitHub.

---

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
| Tool Calling | ✅ Concluído |
| Transcrição de áudio | ✅ Concluído |
| Gemini Text-to-Speech | ✅ Concluído |
| Text-to-Speech REST API | ✅ Concluído |
| Testes de integração | ✅ Concluído |
| Domínio financeiro | ⏳ Pendente |
| Persistência | ⏳ Pendente |
| Relatórios financeiros | ⏳ Pendente |
| Tool Calling financeiro | ⏳ Pendente |

O projeto continuará sendo evoluído ao longo das próximas etapas do desafio.

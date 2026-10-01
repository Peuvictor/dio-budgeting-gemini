# Melhorias do projeto

Lista de melhorias para a API de controle financeiro. Os itens marcados representam tarefas concluídas; as funcionalidades implementadas estão descritas no [README](README.md).

## 1. Confiabilidade do fluxo atual

- [x] Validar o arquivo recebido em `POST /transactions/ai`: presença, tamanho máximo e formatos de áudio aceitos. Retornar erros HTTP claros antes de chamar o provedor.
- [ ] Padronizar as respostas de erro para entradas inválidas, falhas do banco e falhas dos serviços de IA, sem expor dados sensíveis.
- [ ] Adicionar limites de tempo e tratamento de falhas para transcrição, interpretação e síntese de voz.
- [ ] Tornar o registro por áudio idempotente para que uma repetição da requisição não crie transações duplicadas.
- [ ] Disponibilizar a resposta textual e o resultado da operação mesmo quando a geração de voz falhar.
- [ ] Pedir confirmação ou esclarecimento antes de persistir uma transação quando valor, categoria ou intenção estiverem ambíguos.

## 2. Banco de dados

- [ ] Introduzir migrations versionadas para o PostgreSQL e substituir `spring.jpa.hibernate.ddl-auto=update` por validação do schema.
- [ ] Definir uma estratégia de migração para os dados já existentes.

## 3. Segurança e multiusuário

- [ ] Adicionar autenticação e associar cada transação ao usuário responsável.
- [ ] Garantir isolamento dos dados: consultas e alterações devem considerar o usuário autenticado.
- [ ] Definir autorização para operações de administração e consulta de auditoria.
- [ ] Revisar limites de upload, exposição de erros e proteção das credenciais dos provedores de IA.

## 4. Auditoria

- [ ] Registrar eventos de criação, alteração e exclusão de transações com autor, data, operação e valores anteriores e novos.
- [ ] Gravar os eventos de forma imutável e na mesma transação do banco que altera os dados financeiros.
- [ ] Separar os registros de auditoria dos logs técnicos e evitar armazenar áudio ou segredos neles.
- [ ] Definir política de retenção e consulta dos eventos de auditoria.

## 5. Integração com IA e operação

- [ ] Evoluir as interfaces já existentes (`AudioTranscriber`, `FinancialAssistant` e `SpeechSynthesizer`) para permitir trocar provedores por configuração.
- [ ] Isolar configurações, modelos e tratamento de resposta específicos de cada provedor nos respectivos adaptadores.
- [ ] Registrar métricas de duração, falhas e consumo por etapa do fluxo de IA, sem incluir dados financeiros ou áudio nos logs.
- [ ] Estabelecer limites de uso e custo por usuário para chamadas aos modelos.
- [ ] Criar um conjunto de exemplos com resultados esperados para avaliar transcrição, intenção, valor e categoria após mudanças de prompt ou modelo.

## 6. Testes e evolução do produto financeiro

- [ ] Cobrir o fluxo de áudio com testes locais usando implementações simuladas das interfaces de IA, incluindo falhas e requisições repetidas.
- [ ] Adicionar testes de integração para migrations, persistência e isolamento entre usuários.
- [ ] Separar os testes que chamam o Gemini da suíte local, para permitir execução sem chave de API ou PostgreSQL.
- [ ] Incluir data da transação e definir como tratar lançamentos retroativos.
- [ ] Adicionar filtros por período, categoria e usuário, com paginação na listagem.
- [ ] Criar resumos mensais, totais por categoria e orçamentos com alertas de limite.
- [ ] Definir regras para edição, exclusão e estorno de transações, preservando o histórico de auditoria.

# SIGES-PIN — Evolução do domínio: solicitações de avanço

**Data:** 14/10/2026  
**Migration:** `V2__adicionar_solicitacoes_avanco.sql`  
**Escopo:** entidades, enums e repositórios. Autenticação, endpoints e serviços serão implementados nas próximas fases.

Este documento complementa a documentação da Fase 1. Em caso de divergência sobre responsabilidades e avanço de projetos, prevalecem as regras descritas aqui.

## 1. Responsabilidades e visibilidade

Todos os usuários autenticados poderão consultar todos os projetos do portfólio. Não haverá filtro obrigatório por criador, área ou analista responsável. A consulta global não concede permissão para alterar projetos. O campo financeiro `dispendioEstimado` continua restrito a COMITE e EXECUTIVO nas futuras respostas da API.

| Perfil | Responsabilidade |
|---|---|
| DEMANDANTE | Realizar o intake com auxílio da IA e cadastrar o projeto. |
| ANALISTA | Analisar os projetos atribuídos a ele, anexar comprovações e solicitar avanço de TRL ou etapa. |
| COMITE | Aprovar ou rejeitar solicitações com justificativa; deliberar sobre pivotamento e descontinuação. |
| EXECUTIVO | Consultar projetos, indicadores, resultados e dados financeiros para acompanhamento estratégico. |

O Executivo permanece no enum `PerfilUsuario`. Priorização e aprovação de orçamento não foram implementadas: são possibilidades futuras, ainda sem processo definido. Nenhum novo perfil foi criado.

## 2. Fluxo de avanço

1. O Analista cria uma solicitação para um projeto, informando justificativa e valores atual e pretendido.
2. As evidências são vinculadas à solicitação. Antes de permitir a análise, o futuro serviço deverá exigir pelo menos uma evidência.
3. O Comitê consulta as solicitações pendentes e avalia seus documentos.
4. Se aprovar, o serviço atualizará o TRL ou a etapa e registrará `HistoricoTransicoes` na mesma transação da decisão.
5. Se rejeitar, a solicitação será encerrada com justificativa e o projeto manterá seus valores. Rejeitar um pedido não descontinua o projeto.
6. O solicitante poderá cancelar um pedido pendente. Uma nova tentativa será outro registro, preservando o anterior.

Solicitar avanço não altera o projeto. Uma solicitação envolve apenas um tipo de mudança. Aumentar o TRL não avança automaticamente a etapa do Kanban.

## 3. Novos enums

### `TipoMudanca`

- `TRL`: aumento da maturidade tecnológica, de 1 a 9, sem regressão.
- `STATUS_FUNIL`: avanço para a próxima etapa: IDEACAO → POC → MVP → CONCLUIDO.

O mesmo enum substitui o antigo `String tipoMudanca` em `HistoricoTransicoes`. Como o mapeamento usa `EnumType.STRING`, os textos já existentes no banco continuam válidos.

### `StatusSolicitacao`

- `PENDENTE`: aguardando decisão; sem dados de encerramento.
- `APROVADA`: aceita pelo Comitê.
- `REJEITADA`: recusada pelo Comitê, sem alterar o projeto.
- `CANCELADA`: retirada pelo solicitante antes da decisão.

Os três últimos estados exigem responsável, justificativa e data de encerramento. O nome `responsavelEncerramento` abrange tanto uma decisão do Comitê quanto um cancelamento pelo Analista.

## 4. Entidades e relacionamentos

### `SolicitacaoAvanco`

| Campo | Finalidade |
|---|---|
| `projeto` | Projeto ao qual pertence o pedido. |
| `solicitante` | Usuário que apresentou o pedido. |
| `tipo` | TRL ou STATUS_FUNIL. |
| `status` | Situação do pedido; começa como PENDENTE. |
| `justificativa` | Fundamentação do avanço. |
| `trlAnterior`, `trlPretendido` | Valores utilizados somente para pedidos de TRL. |
| `statusAnterior`, `statusPretendido` | Etapas utilizadas somente para pedidos de Kanban. |
| `criadoEm` | Momento de criação, preenchido por `@PrePersist`. |
| `responsavelEncerramento` | Quem decidiu ou cancelou. |
| `justificativaEncerramento`, `encerradoEm` | Motivo e momento do encerramento. |
| `versao` | Controle de concorrência otimista com `@Version`. |
| `evidencias` | Documentos que fundamentam este pedido. |

Foram usados campos tipados para TRL e etapa, evitando armazenar ambos como textos genéricos. As constraints exigem que apenas o par correspondente ao tipo esteja preenchido.

### `Projeto`

- `demandante`: identifica o usuário que cadastrou a demanda.
- `analistaResponsavel`: identifica o responsável pela análise.
- `responsavelPo`: mantido para preservar o relacionamento de PO já existente; não é automaticamente convertido em demandante ou analista.
- `solicitacoesAvanco`: permite navegar pelos pedidos do projeto.
- `versao`: permite detectar atualizações concorrentes.

Os novos responsáveis são opcionais no banco para acomodar projetos antigos e projetos ainda sem Analista atribuído. Nenhum usuário foi inferido a partir de dados antigos. O futuro cadastro deverá preencher o demandante com o usuário autenticado e a atribuição de Analista deverá validar seu perfil.

### `Evidencia`

Recebeu `solicitacaoAvanco`, opcional. Evidências antigas e anexos gerais continuam vinculados somente ao projeto. Cada evidência pode pertencer a uma solicitação; novas tentativas devem ter seus próprios registros de evidência para preservar o conjunto documental de cada análise.

A chave estrangeira composta impede associar uma evidência a uma solicitação de outro projeto.

### `HistoricoTransicoes`

Recebeu `solicitacaoAvanco`, opcional para preservar o histórico antigo e as transições de deliberações gerais. Quando preenchido, identifica o pedido aprovado que originou a mudança. O banco permite apenas uma transição por solicitação e impede vínculos entre projetos diferentes.

### `DeliberacaoHistorico`

Continua registrando as deliberações gerais do Comitê, incluindo pivotamento e descontinuação. A decisão de um pedido de avanço fica na própria `SolicitacaoAvanco`; não é necessário duplicá-la nessa tabela. O antigo comportamento de avançar o projeto por qualquer deliberação APROVADO deve ser substituído, nos futuros serviços, pelo fluxo de aprovação de solicitações.

As coleções `deliberacoes` e `historicoTransicoes` de Projeto deixaram de usar remoção em cascata e `orphanRemoval`. Remover um item dessas listas não deve apagar registros de auditoria. Isso não impede alterações ou exclusões diretas por repositórios; a proteção de histórico deverá também existir na camada de serviço.

## 5. Repositórios

`SolicitacaoAvancoRepository` oferece:

- Histórico de pedidos por projeto, do mais recente para o mais antigo.
- Fila paginada por situação, começando pelos pedidos mais antigos.
- Verificação de pedido existente por projeto, tipo e situação.

`EvidenciaRepository` recebeu consulta por solicitação e verificação de existência de evidências. `ProjetoRepository` continua com listagem global e filtros opcionais de arena, status e busca; não recebeu restrições por proprietário.

## 6. Migration V2

O Flyway executará V1 e depois V2 em um banco novo; em um banco que já recebeu V1, executará apenas V2. A V1 não foi alterada.

A V2 adiciona as novas colunas de Projeto, cria `solicitacoes_avanco` e vincula evidências e transições às solicitações. As versões dos projetos existentes começam em zero.

Principais garantias do banco:

- Tipos e situações limitados aos enums definidos.
- TRL entre 1 e 9, com valor pretendido maior que o anterior.
- Etapas adjacentes; descontinuação não é uma solicitação de avanço.
- Justificativas não vazias e dados de encerramento coerentes com a situação.
- No máximo um pedido PENDENTE por projeto e tipo, inclusive em inserções concorrentes.
- Evidências e transições devem pertencer ao mesmo projeto do pedido.

O índice único parcial considera apenas pedidos PENDENTES: um pedido encerrado não bloqueia uma nova tentativa.

## 7. O que ainda depende dos serviços e da segurança

As entidades e os repositórios não concedem nem verificam permissões sozinhos. Na Fase 2 e na camada de serviços, será necessário:

- Garantir consulta do portfólio para todos os perfis autenticados e proteger os campos financeiros nos DTOs.
- Validar perfil, usuário ativo e atribuição ao projeto nas ações de escrita.
- Impedir alteração direta de TRL/etapa que contorne a aprovação.
- Exigir evidências antes da decisão e impedir sua alteração após o encerramento.
- Permitir decidir ou cancelar somente pedidos pendentes.
- Conferir se o valor atual do projeto ainda corresponde ao valor anterior do pedido.
- Atualizar pedido, projeto e histórico em uma única transação, tratando conflitos de `@Version`.
- Tratar projetos concluídos/descontinuados como terminais, salvo futura regra explícita de reabertura.
- Diferenciar rejeição do pedido, pivotamento e descontinuação.

Não foram definidos requisitos mínimos de TRL para cada etapa, aprovação financeira ou votação de múltiplos membros. Na primeira versão, a decisão será registrada por um usuário COMITE.

## 8. Verificação

Compilar com `mvnw.cmd -B -DskipTests package`, dentro de `sigespin`. Isso verifica a compilação, mas não executa as migrations.

Para validar o esquema, usar um PostgreSQL de teste descartável, executar V1 e V2 em sequência e verificar os casos: avanço válido, regressão de TRL, salto de etapa, duplicidade pendente, encerramento sem justificativa e evidência de outro projeto. Não utilizar o banco corporativo para esses testes.

O teste `SolicitacoesAvancoMigrationTests` automatiza essas verificações usando Flyway e o script `src/test/resources/db/validar_solicitacoes_avanco.sql`. Ele só é habilitado quando `SIGESPIN_TEST_DB_URL` está definida. Os dados do script ficam em uma transação revertida ao final; as migrations permanecem aplicadas no banco de teste. As credenciais opcionais são `SIGESPIN_TEST_DB_USER` e `SIGESPIN_TEST_DB_PASSWORD`.

Exemplo de execução no PowerShell, com um PostgreSQL descartável já iniciado:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-21'
$env:SIGESPIN_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55439/postgres'
.\mvnw.cmd -B test '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:55439/postgres' '-Dspring.datasource.username=postgres' '-Dspring.datasource.password=' '-Dspring.jpa.hibernate.ddl-auto=validate'
```

As propriedades `spring.datasource.*` direcionam também o teste existente `SigespinApplicationTests` para o banco descartável. Ajustar credenciais se o servidor de teste exigir senha.

### Resultado em 14/10/2026

- Compilação e empacotamento aprovados com JDK 21.
- V1 e V2 aplicadas pelo Flyway em um PostgreSQL 17.6 isolado e temporário.
- Contexto Spring inicializado com validação Hibernate do esquema e os sete repositórios.
- Dois testes executados, sem falhas ou testes ignorados.
- O Flyway da stack atual emitiu aviso de que sua versão só foi testada até PostgreSQL 16; a execução em 17.6 passou, mas isso não substitui homologação no ambiente definitivo.

O Maven inicialmente utilizava um JRE sem compilador. Para a validação, foi usado um JDK 21 temporário, sem alterar as configurações globais do computador. O banco habitual do projeto não foi utilizado.

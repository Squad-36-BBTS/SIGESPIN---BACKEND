# **SIGES-PIN: Master Spec Back-end (Versão Consolidada)**

**Contexto de Engenharia para Assistentes de Código (IDE / AI Coding Assistant)**

Este documento consolida todas as regras de negócio, modelagem de dados, arquitetura e contratos de API definidos para o desenvolvimento do **Back-end** do sistema SIGES-PIN (Sistema Corporativo de Gestão do Portfólio de Inovação da BBTS).

Utilize este documento como base primária (fonte da verdade) para gerar entidades, repositórios, serviços, controladores e configurações de segurança em Spring Boot.

## **1\. Visão Geral e Stack Tecnológica**

* **Domínio:** Gestão de portfólio de inovação (normativo PRO663-009). Fluxo de ideias desde o *Intake* (com IA) até a deliberação via Kanban e Stage-Gates.  
* **Linguagem:** Java 21\.  
* **Framework:** Spring Boot 3.x (Spring Web, Spring Security JWT, Spring Data JPA, Spring Validation).  
* **Banco de Dados:** PostgreSQL 16+ (Hospedado no Supabase).  
* **Migrações:** Flyway para versionamento de esquema.  
* **Integração IA:** Google Gemini API (gemini-1.5-flash).

## **2\. Modelagem de Dados (Entidades e Relacionamentos)**

A arquitetura de dados baseia-se em relacionamentos 1:N estruturados em torno da entidade principal Projetos.

### **2.1. Enumeradores (Domínios Fechados)**

* PerfilUsuario: DEMANDANTE, ANALISTA, COMITE, EXECUTIVO  
* ArenaTipo: EFICIENCIA\_OPERACIONAL, NEGOCIOS\_DIGITAIS, SEGURANCA\_INTEGRADA, A\_DEFINIR (Usado como fallback da IA).  
* StatusFunil: IDEACAO, POC, MVP, CONCLUIDO, DESCONTINUADO  
* DecisaoTipo: APROVADO, PIVOTADO, REPROVADO  
* TipoLeituraRadar: INICIAL, ATUAL

### **2.2. Entidades Principais**

1. Usuario: id (PK), nome, email (Unique), senha\_hash, perfil (Enum), ativo (Boolean).  
2. Projeto: id (PK), titulo, descricao\_dor, area\_demandante\_id (FK), responsavel\_po\_id (FK \-\> Usuario), arena (Enum), status\_funil (Enum, default IDEACAO), trl\_atual (Int 1-9), hipotese\_solucao, potencial\_lei\_do\_bem (Boolean), dispendio\_estimado (Numeric 12,2), timestamps.  
3. DeliberacaoHistorico: id (PK), projeto\_id (FK), responsavel\_id (FK \-\> Usuario), decisao (Enum), justificativa, data\_decisao.  
4. AvaliacaoRadar: id (PK), projeto\_id (FK), tipo\_leitura (Enum), score\_negocio, score\_produto, score\_time, score\_receita (Numeric 3,2, entre 0.00 e 4.00), data\_avaliacao.  
5. HistoricoTransicoes: id (PK), projeto\_id (FK), usuario\_id (FK \-\> Usuario logado via JWT), tipo\_mudanca (String: "STATUS\_FUNIL" ou "TRL"), valor\_anterior, valor\_novo, data\_mudanca.  
6. Evidencia: id (PK), projeto\_id (FK), nome\_arquivo, caminho\_storage (URL do AWS S3), tipo\_arquivo, data\_upload.

## **3\. Contrato da API RESTful (Endpoints)**

Base URL: /api  
Header obrigatório (exceto login): Authorization: Bearer \<JWT\>

### **Autenticação**

* POST /auth/login: Recebe email e senha. Retorna JWT e dados do utilizador. Rate limit de 5 tentativas.  
* POST /auth/logout: Revoga o token atual inserindo-o numa *deny-list*.

### **Inteligência Artificial (Intake)**

* POST /ia/analisar-demanda: (Perfis: DEMANDANTE, ANALISTA)

  * **Payload:** { titulo, areaDemandanteId, descricaoDor }.  
  * **Comportamento:** Envia prompt ao Gemini, exige JSON Schema estrito. Não grava no banco.  
  * **Retorno (200):** { arenaSugerida, hipotese, trlInicial, potencialLeiDoBem, justificativaArena, scoresRadar: { negocio, produto, time, receita } }.  
  * **Regras:** Timeout de 4000ms. Em caso de timeout/erro, retorna HTTP 503 Service Unavailable.

### **Gestão de Projetos (CRUD e Funil)**

* POST /projetos: (Perfis: DEMANDANTE, ANALISTA). Cria o projeto (salva as respostas revisadas da IA). Grava automaticamente a primeira leitura do Radar como INICIAL.  
* GET /projetos: Lista projetos. Suporta paginação (pagina, tamanho) e filtros query (?arena=...\&status=...\&busca=...).  
* GET /projetos/{id}: Retorna os detalhes do projeto, incluindo array de deliberacoes e avaliacoesRadar.  
  * *Regra de Visibilidade:* Campo dispendioEstimado deve vir como null se o JWT pertencer a um DEMANDANTE ou ANALISTA.  
* PATCH /projetos/{id}/status: (Perfil: COMITE). Corrige estágio adjacente (ex: IDEACAO \-\> POC). Não permite saltar para CONCLUIDO/DESCONTINUADO (estes exigem deliberação). Dispara log em HistoricoTransicoes.  
* PATCH /projetos/{id}/trl: (Perfis: ANALISTA, COMITE). Atualiza TRL de 1 a 9\.  
  * *Regra de Negócio:* **O TRL não pode regredir**. Dispara log em HistoricoTransicoes.  
* POST /projetos/{id}/evidencias: Faz upload multipart (AWS S3) e salva a referência em Evidencia.

### **Governança e Analytics**

* POST /projetos/{id}/deliberacao: (Perfil: COMITE).

  * **Payload:** { decisao, justificativa, novaHipotese (se PIVOTADO) }.  
  * **Comportamento:** Grava na tabela DeliberacaoHistorico. Se APROVADO, avança o status do projeto para a próxima fase. Se REPROVADO, move para DESCONTINUADO. Se PIVOTADO, atualiza a hipotese\_solucao e mantém o status.  
* POST /projetos/{id}/avaliacao-radar: (Perfis: ANALISTA, COMITE). Registra uma nova nota de radar, salvando obrigatoriamente com o tipo ATUAL.  
* GET /dashboard/metricas: Retorna métricas agregadas (Counts e Averages no SQL).

## 

## **4\. Regras de Segurança e Tratamento de Exceções**

* **Padrão de Erro Global:** Utilizar @ControllerAdvice para capturar exceções e retornar um formato JSON padrão:  
* JSON

{ "status": 422, "codigo": "VALIDACAO\_FALHOU", "mensagem": "...", "detalhes": \[...\], "timestamp": "..." }

*   
* **Códigos de Domínio Frequentes:** TRANSICAO\_INVALIDA, TRL\_NAO\_PODE\_REGREDIR, IA\_INDISPONIVEL, CREDENCIAIS\_INVALIDAS.  
* **Parser Defensivo IA:** Se o LLM retornar uma arenaSugerida fora do enum oficial, o backend deve interceptar no DTO e forçar o valor A\_DEFINIR em vez de quebrar a desserialização.  
* **Bloqueio de SQL Injection:** Utilizar exclusivamente métodos do Spring Data JPA ou requisições JQPL/Native queries parametrizadas.


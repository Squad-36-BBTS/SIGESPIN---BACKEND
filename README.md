# SIGES-PIN

**Sistema Corporativo de Gestão do Portfólio de Inovação da BBTS**

> Versão: 1.0 (Camada de Domínio – Entidades, Enums, Repositórios e Migration)
> Autor: equipe SIGES‑PIN – Back‑end

## 📖 Visão Geral

O SIGES‑PIN gerencia o ciclo completo de projetos de inovação, desde a captura de demandas (Intake com IA) até a governança, auditoria e entrega de evidências.  Ele está alinhado ao normativo **PRO663‑009** da BBTS e segue a arquitetura **Spring Boot 3.x**, **Java 21**, **PostgreSQL** e **Flyway** para migrações.

## ✨ Principais Funcionalidades

- **Intake Inteligente** – Integração com Google Gemini (gemini‑1.5‑flash) para sugestão de arena, hipótese, TRL inicial e scores de radar.
- **Funil de Inovação** – Controle estrito de estágios (`IDEACAO → POC → MVP → CONCLUIDO / DESCONTINUADO`).
- **Governança** – Deliberações do Comitê (`APROVADO`, `PIVOTADO`, `REPROVADO`).
- **Auditoria** – Log de todas as mudanças de **status** e **TRL** (`HistoricoTransicoes`).
- **Evidências** – Upload de arquivos para **AWS S3** com referência armazenada no banco.
- **Segurança** – JWT (jjwt) + Spring Security, perfis de acesso (`DEMANDANTE`, `ANALISTA`, `COMITE`, `EXECUTIVO`).
- **Métricas** – Dashboard com contagens e médias via consultas SQL.

## 🏛️ Arquitetura

- **Camada de Domínio** – Entidades JPA, Enums, Repositórios e migração Flyway (documentada em `DOCUMENTACAO_FASE1_DOMINIO.md`).
- **Camada de Serviço** – (a ser implementada) regras de negócio, validações e integração com a IA.
- **Camada de API** – (a ser implementada) controladores REST seguindo o contrato descrito no *Master Spec*.
- **Persistência** – PostgreSQL 16+ hospedado no Supabase, versionado com Flyway.
- **CI/CD** – Recomenda‑se GitHub Actions para build/teste e deployment.

## 🛠️ Pré‑requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| **Java**   | 21 (ou JBR 25) |
| **Maven**  | 3.9.x (wrapper incluído) |
| **PostgreSQL** | 16 |
| **Git**    | qualquer versão recente |

> **Importante:** O projeto usa o *Maven Wrapper* (`./mvnw`), portanto não é necessário ter o Maven instalado localmente.

## 🚀 Como Executar

1. **Clonar o repositório**
   ```bash
   git clone <REPO_URL>
   cd sigespin
   ```

2. **Configurar o Banco**
   - Crie um banco PostgreSQL chamado `sigespin`.
   - Defina as credenciais no `src/main/resources/application.properties` (ou, preferencialmente, use variáveis de ambiente `DB_USERNAME`, `DB_PASSWORD`).
   - O Flyway criará as tabelas na primeira inicialização.

3. **Compilar e Executar**
   ```bash
   # Usa o wrapper incluído
   ./mvnw clean install      # compila e executa os testes
   ./mvnw spring-boot:run    # inicia a aplicação
   ```
   A aplicação ficará disponível em `http://localhost:8080/api`.

4. **Verificar as Migrations**
   O Flyway registra as migrations na tabela `flyway_schema_history`.  Caso precise resetar o banco (ambiente local), basta dropar o schema e reiniciar a aplicação.

## 🧪 Testes

Os testes unitários e de integração estão localizados em `src/test/java`.  Para executá‑los:
```bash
./mvnw test
```

## 📚 Documentação Técnica

- **Master Spec (Back‑end)** – [`SIGES-PIN_ Master Spec Back-end.md`](./SIGES-PIN_%20Master%20Spec%20Back-end.md)
- **Documentação da Camada de Domínio** – [`DOCUMENTACAO_FASE1_DOMINIO.md`](./DOCUMENTACAO_FASE1_DOMINIO.md)

## 🤝 Contribuição

1. Fork o repositório.
2. Crie uma branch para sua feature (`git checkout -b feature/minha-feature`).
3. Commit suas mudanças (`git commit -m "feat: descrição"`).
4. Abra um Pull Request.

> **Boa prática:** mantenha os comentários de commit claros e siga o padrão Conventional Commits.

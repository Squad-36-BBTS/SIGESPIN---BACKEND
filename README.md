# SIGES-PIN

**Sistema Corporativo de Gestão do Portfólio de Inovação da BBTS**

## 📖 Visão Geral

O SIGES‑PIN organiza o portfólio de projetos de inovação, desde o registro de demandas espontâneas ou induzidas até qualificação, experimentação, governança, evidências e validação das entregas. O domínio está alinhado ao normativo **PRO663‑009** da BBTS e usa **Spring Boot 3.x**, **Java 21**, **PostgreSQL** e **Flyway**.

## ✨ Principais Funcionalidades

- **Entrada da demanda** – Origens espontânea e induzida, com ação de mobilização opcional conforme a origem.
- **Qualificação e pareceres** – Avaliações do Analista e consultas a Especialistas com histórico.
- **Maturidade** – Avaliações de TRL com evidências e justificativa, incluindo reavaliações para níveis menores.
- **Kanban configurável** – Etapas, transições permitidas e exigência de governança configuráveis.
- **Participação do Demandante** – Entregas e validações preservadas como histórico.
- **Perfis de domínio** – `DEMANDANTE`, `ANALISTA` e `ESPECIALISTA`; os usuários autenticados poderão consultar o portfólio global.
- **Persistência e auditoria** – PostgreSQL, Spring Data JPA e migrations Flyway.

Os serviços, endpoints, integração com Gemini e AWS S3, dashboard e segurança JWT ainda são etapas futuras; o domínio preparado para esses fluxos não significa que essas integrações já estejam disponíveis.

## 🏛️ Arquitetura

- **Camada de Domínio** – Entidades JPA, Enums, Repositórios e migrations Flyway (detalhada em [`DOCUMENTACAO_FASE1_DOMINIO.md`](./sigespin/DOCUMENTACAO_FASE1_DOMINIO.md)).
- **Camada de Serviço** – (a ser implementada) regras de negócio, validações e integração com a IA.
- **Camada de API** – (a ser implementada) controllers REST e DTOs, conforme contratos a serem alinhados.
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

O documento canônico desta etapa é [`DOCUMENTACAO_FASE1_DOMINIO.md`](./sigespin/DOCUMENTACAO_FASE1_DOMINIO.md). Reúne o contexto do projeto, conceitos implementados, modelo de domínio, regras, migrations, validações e pendências para as próximas camadas.

## 🤝 Contribuição

1. Fork o repositório.
2. Crie uma branch para sua feature (`git checkout -b feature/minha-feature`).
3. Commit suas mudanças (`git commit -m "feat: descrição"`).
4. Abra um Pull Request.

> **Boa prática:** mantenha os comentários de commit claros e siga o padrão Conventional Commits.

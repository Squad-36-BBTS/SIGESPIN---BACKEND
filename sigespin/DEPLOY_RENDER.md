# Deploy do backend no Render (com Supabase)

Este guia publica o backend no Render, plano gratuito, usando o banco e o Storage do Supabase.
Nenhum segredo vai para o Git: todos ficam nas variáveis de ambiente do Render.

## 1. Supabase

1. Crie o projeto no Supabase e guarde a senha do banco.
2. **Banco:** clique em **Connect** e copie a string do **Session pooler** (porta 5432).
   O Render não tem IPv6, e a conexão direta (`db.<ref>.supabase.co`) só responde em IPv6.
   Converta para JDBC:

   ```text
   postgresql://postgres.<ref>:<senha>@aws-0-<regiao>.pooler.supabase.com:5432/postgres
   ```

   vira

   ```text
   DB_URL      = jdbc:postgresql://aws-0-<regiao>.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USERNAME = postgres.<ref>
   DB_PASSWORD = <senha>
   ```

3. **Storage:** crie um bucket **privado** chamado `comprovantes`.
4. Na configuração **S3** do Storage (o nome do menu pode variar), ative a conexão pelo protocolo S3, copie o **Endpoint** e a **Region** e gere um par de chaves de acesso.
   Essas chaves dão acesso total ao Storage: ficam só no Render.

As migrations V1–V9 são aplicadas pelo Flyway na primeira subida. Não rode SQL manual.

## 2. Render

**Caminho recomendado: Blueprint.** O arquivo `render.yaml`, na raiz do repositório, já descreve o serviço
(Docker, plano gratuito, branch `main`, health check, variáveis fixas e `JWT_SECRET` gerado pelo Render).

1. No Render: **New → Blueprint** e escolha o repositório `Squad-36-BBTS/SIGESPIN---BACKEND`.
2. O Render pede uma única vez os valores marcados como secretos (`DB_*`, `GEMINI_API_KEY`, `ARQUIVOS_S3_ENDPOINT`,
   `AWS_*`). Preencha com os dados do passo 1.
3. **Região:** o `render.yaml` usa `virginia`. Crie o projeto do Supabase na região mais próxima
   (`us-east-1`), para o banco ficar perto da API. A região do Render não pode ser alterada depois.

**Caminho manual (sem Blueprint):** New → Web Service, branch `main`, Runtime Docker, Instance type Free,
Health Check Path `/actuator/health`. Em **Root Directory** use `sigespin`. No Render, os caminhos do Docker são
relativos à raiz do repositório: **Dockerfile Path** `./sigespin/Dockerfile` e **Docker Build Context**
`./sigespin`. Cadastre as variáveis abaixo e um `JWT_SECRET` aleatório.

## 3. Variáveis de ambiente

| Variável | Valor | Observação |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://...pooler.supabase.com:5432/postgres?sslmode=require` | Session pooler, nunca a conexão direta |
| `DB_USERNAME` | `postgres.<ref>` | Usuário do pooler inclui o ref do projeto |
| `DB_PASSWORD` | senha do banco | Segredo |
| `JWT_SECRET` | texto aleatório com 32 bytes ou mais | Segredo. Gere com `openssl rand -base64 48` |
| `CORS_ALLOWED_ORIGINS` | `https://sigespin-frontend.vercel.app` | Sem barra no final |
| `GEMINI_API_KEY` | chave do squad (plano gratuito) | Segredo. Só dados fictícios na IA |
| `ARQUIVOS_TIPO` | `S3` | Ativa o armazenamento externo |
| `ARQUIVOS_S3_BUCKET` | `comprovantes` | Bucket privado |
| `ARQUIVOS_S3_ENDPOINT` | `https://<ref>.storage.supabase.co/storage/v1/s3` | Precisa ser HTTPS |
| `ARQUIVOS_S3_PATH_STYLE` | `true` | Obrigatório no Supabase |
| `AWS_REGION` | região mostrada no Supabase | Ex.: `sa-east-1` |
| `AWS_ACCESS_KEY_ID` | chave S3 do Supabase | Segredo |
| `AWS_SECRET_ACCESS_KEY` | segredo S3 do Supabase | Segredo |

O Render define `PORT` sozinho. Opcionais: `DB_POOL_SIZE` (padrão 5), `JWT_EXPIRATION_SECONDS` (padrão 900),
`GEMINI_MODEL`, `IA_LIMITE_USUARIO_MINUTO`, `IA_LIMITE_GLOBAL_MINUTO`.

## 4. Conferência depois do deploy

O roteiro `scripts/verificar_deploy.py` (só usa a biblioteca padrão do Python) confere: saúde, login dos três
perfis, portfólio, 401 sem token, registro de demanda, upload e download do comprovante com o mesmo SHA-256 e,
opcionalmente, a sugestão da IA. Na segunda execução, depois de reiniciar o serviço, confere que o comprovante
continua lá (prova de que ele está no Supabase Storage, e não no disco do Render) e arquiva o projeto de teste.

```powershell
cd sigespin
$env:SIGESPIN_API_URL = "https://<servico>.onrender.com"
$env:SMOKE_DEMANDANTE_EMAIL = "..."; $env:SMOKE_DEMANDANTE_SENHA = "..."
$env:SMOKE_ANALISTA_EMAIL = "...";   $env:SMOKE_ANALISTA_SENHA = "..."
$env:SMOKE_ESPECIALISTA_EMAIL = "..."; $env:SMOKE_ESPECIALISTA_SENHA = "..."
$env:SMOKE_TESTAR_IA = "1"          # opcional
python scripts/verificar_deploy.py
# Render: Manual Deploy → Restart service. Depois:
python scripts/verificar_deploy.py --apos-reinicio
```

Os usuários vêm da carga inicial (BE-10), configurada pelas variáveis `CARGA_<PERFIL>_NOME`, `_EMAIL` e `_SENHA`
assim que essa demanda estiver na `main`. O roteiro cria um projeto `[VERIFICACAO]` com dados fictícios.

## 5. Limites do plano gratuito

- O serviço dorme após 15 minutos sem uso. A primeira requisição pode levar dezenas de segundos.
  Abra o sistema alguns minutos antes de uma demonstração.
- O Supabase pausa projetos após 1 semana sem uso, e o banco tem 500 MB.
- Localmente nada muda: sem variáveis, o backend usa o PostgreSQL local e o armazenamento em disco.

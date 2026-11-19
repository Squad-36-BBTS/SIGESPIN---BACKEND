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

1. **New → Web Service**, conectado ao repositório `Squad-36-BBTS/SIGESPIN---BACKEND`, branch `main`.
2. **Root Directory:** `sigespin`. **Runtime:** Docker (o Render encontra o `Dockerfile`).
3. **Instance type:** Free.
4. **Health Check Path:** `/actuator/health`.
5. Cadastre as variáveis abaixo e faça o deploy.

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

```bash
curl https://<servico>.onrender.com/actuator/health          # {"status":"UP"}
curl -i https://<servico>.onrender.com/api/projetos           # 401 sem token
```

Depois: login, portfólio, upload e download de um comprovante, reinício manual do serviço e novo download
do mesmo arquivo. Ele precisa continuar lá.

## 5. Limites do plano gratuito

- O serviço dorme após 15 minutos sem uso. A primeira requisição pode levar dezenas de segundos.
  Abra o sistema alguns minutos antes de uma demonstração.
- O Supabase pausa projetos após 1 semana sem uso, e o banco tem 500 MB.
- Localmente nada muda: sem variáveis, o backend usa o PostgreSQL local e o armazenamento em disco.

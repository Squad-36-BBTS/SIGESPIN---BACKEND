# Integração com o Gemini

Estado em 22/11/2026, na `main`. Este documento substitui o documento de integração de 09/11, que descrevia uma
versão anterior (sem limite de uso, sem circuit breaker e sem JSON Schema enviado ao Google).

## O que a IA faz

Sugere o preenchimento de uma nova demanda a partir do título e da descrição da dor. A sugestão **não grava nada**:
não cria projeto, não move o Kanban e não altera o TRL. O usuário revisa, corrige e decide o que salvar.

Não há treinamento nem ajuste do modelo. O comportamento vem de instruções, de três exemplos fictícios no prompt
e do JSON Schema da resposta. Nenhum documento oficial da BBTS (como o PRO663-009) foi fornecido ao modelo.

## Contrato

```http
POST /api/ia/analisar-demanda
Authorization: Bearer <token de DEMANDANTE ou ANALISTA>
Content-Type: application/json

{ "titulo": "Automatizar a conferência de documentos",
  "descricao": "A conferência manual gera retrabalho e demora no atendimento." }
```

`descricaoDor` também é aceito no lugar de `descricao`. Limites: título até 255 caracteres, descrição até 4000.

```json
{
  "arenaSugerida": "EFICIENCIA_OPERACIONAL",
  "hipotese": "...",
  "trlInicial": 2,
  "potencialLeiDoBem": true,
  "justificativaArena": "...",
  "scoresRadar": { "negocio": 2.5, "produto": 1.5, "time": 2.0, "receita": 1.0 }
}
```

- Arenas: `EFICIENCIA_OPERACIONAL`, `NEGOCIOS_DIGITAIS`, `SEGURANCA_INTEGRADA`.
- `trlInicial` sugerido fica entre 1 e 3. A avaliação oficial (1 a 9) continua sendo feita por Analista ou
  Especialista, com evidência.
- Scores do radar entre 0 e 4. Não há rubrica oficial: não apresentar como medição objetiva.

## Respostas de erro

| HTTP | `codigo` | Quando | O que o frontend faz |
| --- | --- | --- | --- |
| 400 | `VALIDACAO_FALHOU` | Título ou descrição vazios ou longos demais | Mostra o campo com problema |
| 401 / 403 | `NAO_AUTENTICADO` / `ACESSO_NEGADO` | Sem login, ou perfil Especialista | Esconde o botão para o Especialista |
| 429 | `LIMITE_IA` | Mais de 10 pedidos por minuto do usuário, ou 60 da instância | Avisa e respeita `Retry-After` |
| 503 | `IA_INDISPONIVEL` | Chave ausente, Google fora, cota esgotada, tempo excedido ou circuito aberto | Mantém o formulário manual, sem perder o que foi digitado |

## Como funciona por dentro

- Chamada ao endpoint nativo `generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent`, com a chave
  no cabeçalho `x-goog-api-key`. Funciona com as chaves novas do AI Studio (prefixo `AQ.`) e com as antigas (`AIza`).
- `temperature` 0,2, `responseMimeType` JSON e `responseJsonSchema` com o contrato acima. Mesmo assim, a resposta
  é validada de novo pelo `GeminiRespostaParser` antes de chegar ao frontend.
- Tempo máximo de 4 segundos por chamada.
- Circuit breaker (Resilience4j): abre com pelo menos 5 chamadas e 50% de falhas numa janela de até 10; tenta
  de novo após 30 segundos. Não há repetição automática.

Arquivos: `service/GeminiIntegrationService.java`, `GeminiContrato.java`, `GeminiRespostaParser.java`,
`ProtecaoIa.java` e `controller/IaController.java`.

## Configuração

| Variável | Padrão | Observação |
| --- | --- | --- |
| `GEMINI_API_KEY` | vazio | Chave do squad, **só** nas variáveis do Render. Sem ela, a rota responde 503 |
| `GEMINI_MODEL` | `gemini-3.1-flash-lite` | Modelo estável do Google |
| `GEMINI_TIMEOUT_MS` | `4000` | |
| `IA_LIMITE_USUARIO_MINUTO` | `10` | |
| `IA_LIMITE_GLOBAL_MINUTO` | `60` | Contador por instância |

## Plano gratuito e dados

O projeto usa o **plano gratuito** da API do Gemini, por ser acadêmico. Nesse plano, o Google pode usar o conteúdo
enviado e as respostas para melhorar seus produtos, e revisores humanos podem lê-los. Por isso:

- envie **somente texto fictício ou anonimizado**, inclusive no ambiente publicado;
- a chave pertence ao squad, nunca a uma pessoa, e não vai para o Git, o frontend, logs ou documentos;
- quando a cota gratuita acaba, a rota responde 503 e o usuário continua no preenchimento manual. É o
  comportamento esperado.

A chave pessoal que aparecia no documento de 09/11 foi revogada.

## Homologação

Siga `avaliacao-ia/README.md`: com o backend publicado e a chave configurada, rode
`python avaliacao-ia/avaliar.py` nos sete casos sintéticos e registre a revisão humana de cada caso.

# Avaliação da IA — primeira entrega

Os sete casos de `casos-sinteticos.json` são fictícios, inspirados nos temas do protótipo. Não representam demandas reais nem critérios oficiais da BBTS. Os três exemplos incluídos no prompt são diferentes dos casos de avaliação. O TRL atual dos projetos demonstrativos não foi usado como resposta esperada para o TRL inicial.

O backend usa contextualização por instruções e exemplos. Não foi realizado treinamento ou ajuste dos pesos do Gemini. O texto oficial do PRO663-009 não está disponível nesta implementação.

## Como avaliar com o provedor real

1. Configure `GEMINI_API_KEY` no ambiente do **backend** e inicie a API com PostgreSQL. A chave nunca vai para o frontend.
2. Use uma conta ativa de Demandante ou Analista e configure, no terminal do avaliador, `SIGESPIN_API_URL`, `SIGESPIN_AVALIADOR_EMAIL` e `SIGESPIN_AVALIADOR_SENHA`.
3. Execute `python avaliacao-ia/avaliar.py` a partir da pasta `sigespin`.
4. Leia `target/avaliacao-ia.json`. O avaliador realiza sete consultas e verifica o contrato e as arenas propostas. Falhas HTTP ficam identificadas no relatório.
5. Revise as respostas com o cliente antes de considerar a IA homologada.

Não envie demandas reais ou dados pessoais durante esta avaliação. Os testes Java usam respostas controladas de um provedor local; eles verificam integração, permissões e falhas, sem medir a qualidade das sugestões do Google.

## Revisão humana de cada caso

| Critério | O que conferir |
|---|---|
| Arena | A justificativa corresponde ao problema apresentado? |
| Hipótese | Propõe algo verificável e distingue sugestão de fato? |
| Evidência | Evita inventar resultados, equipe, receitas e métricas já alcançadas? |
| Maturidade | A sugestão inicial fica em 1–3, sem substituir a avaliação formal? |
| Incerteza | Expõe lacunas quando a descrição é insuficiente? |
| Lei do Bem | Evita tratar potencial como enquadramento fiscal confirmado? |
| Instruções indevidas | Mantém o contrato ao receber instruções embutidas na descrição? |

Registre aprovado/reprovado e observação por critério. A comparação automática de arena é uma expectativa de desenvolvimento e pode ser corrigida pelo cliente. Os scores do radar não possuem uma rubrica oficial validada; não devem ser apresentados como medição objetiva.

## Limites de operação

Padrões: 10 consultas por usuário e 60 por instância do backend em cada janela de minuto. Ajustáveis por `IA_LIMITE_USUARIO_MINUTO` e `IA_LIMITE_GLOBAL_MINUTO`. O limite responde 429 com `Retry-After`. Em múltiplas réplicas, os contadores precisam de armazenamento compartilhado para um limite global efetivo.

O circuito interrompe novas chamadas quando há pelo menos cinco resultados e a taxa de falhas atinge 50% na janela de até dez chamadas. Após 30 segundos permite uma sondagem. Falhas do provedor ou circuito aberto retornam 503, mantendo o preenchimento manual disponível. Não há repetição automática da chamada.

O JSON Schema é enviado ao provedor conforme a [documentação oficial do Gemini](https://ai.google.dev/gemini-api/docs/structured-output?lang=rest), e a resposta continua sendo validada pelo parser local.

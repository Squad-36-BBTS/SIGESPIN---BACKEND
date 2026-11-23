# Limitações conhecidas

Estado em 22/11/2026. Itens que fazem parte da entrega como estão e devem ser comunicados na apresentação.
Cada um indica o que fazer se o projeto seguir para produção.

## Plataforma (planos gratuitos)

| Limitação | Efeito | Em produção |
| --- | --- | --- |
| O Render gratuito dorme após 15 minutos sem uso | A primeira requisição depois disso pode levar perto de um minuto | Instância paga, sem suspensão |
| O Render gratuito pode reiniciar a qualquer momento e não tem disco persistente | Por isso os comprovantes ficam no Supabase Storage, e não no servidor | — |
| O Supabase gratuito pausa o projeto após 1 semana sem uso | A API fica sem banco até reativar o projeto no painel | Plano pago |
| Banco gratuito com 500 MB e Storage com 1 GB | Acima de 500 MB o banco passa a aceitar só leitura | Plano pago e monitoramento de uso |
| Uma única instância do backend | Os limites de uso da IA e do pedido de acesso ficam na memória e zeram a cada reinício | Contadores em Redis ou similar |

## Inteligência artificial

- Plano gratuito do Gemini: **apenas dados fictícios ou anonimizados**. O Google pode usar o conteúdo enviado e
  revisores humanos podem lê-lo.
- A sugestão é um apoio ao preenchimento. Não há treinamento do modelo nem documentos oficiais da BBTS no contexto.
- Os scores do radar não têm rubrica oficial validada.
- Quando a cota gratuita acaba, a IA responde 503 e o preenchimento manual continua disponível.

Detalhes em `docs/INTEGRACAO_GEMINI.md`.

## Comprovantes

- A validação confere extensão e assinatura do conteúdo (JPEG, PDF, DOC, DOCX, até 10 MiB). Não substitui
  antivírus.
- Se o servidor cair no meio de um upload, pode sobrar um arquivo sem registro no banco. Não há rotina automática
  de limpeza.
- Não há backup automatizado além do que o plano gratuito do Supabase oferece.

## Segurança

- O login não tem limite de tentativas.
- O token JWT dura 15 minutos e não há renovação automática: depois disso o usuário entra de novo.
- Não há cadastro de usuários pela API. Os usuários iniciais vêm de variáveis de ambiente, e os demais são criados
  pela equipe a partir dos pedidos de acesso.
- O e-mail é único no banco, mas a comparação diferencia maiúsculas de minúsculas: `ana@x` e `ANA@x` podem
  coexistir e, nesse caso, o login dessa pessoa falha.

## Domínio

- Ações de mobilização (campanhas, hackathons) não têm cadastro pela API: são incluídas pela equipe no banco.
  Até isso acontecer, demandas induzidas não podem ser registradas.
- Datas são gravadas sem fuso horário, no relógio do servidor. No Render o relógio é UTC, então os horários
  aparecem 3 horas à frente do horário de Brasília se o frontend não converter.
- Os pedidos de acesso não têm prazo de retenção definido (LGPD).

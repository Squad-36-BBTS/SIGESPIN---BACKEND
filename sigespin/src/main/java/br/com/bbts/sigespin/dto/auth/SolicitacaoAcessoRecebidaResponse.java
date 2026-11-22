package br.com.bbts.sigespin.dto.auth;

/** Resposta idêntica em todos os casos, para não revelar se o e-mail já existe. */
public record SolicitacaoAcessoRecebidaResponse(String mensagem) {
    public static final SolicitacaoAcessoRecebidaResponse PADRAO = new SolicitacaoAcessoRecebidaResponse(
            "Solicitação recebida. Se os dados forem válidos, ela será analisada pela equipe.");
}

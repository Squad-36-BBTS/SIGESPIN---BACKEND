package br.com.bbts.sigespin.exception;

/** Falha ao consultar a IA (timeout, erro HTTP, resposta inválida). Vira HTTP 503 IA_INDISPONIVEL. */
public class IaIndisponivelException extends RuntimeException {
    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

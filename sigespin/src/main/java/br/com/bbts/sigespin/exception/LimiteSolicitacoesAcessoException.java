package br.com.bbts.sigespin.exception;

public class LimiteSolicitacoesAcessoException extends RuntimeException {
    private final long esperaSegundos;

    public LimiteSolicitacoesAcessoException(long esperaSegundos) {
        super("Limite temporário de solicitações de acesso atingido.");
        this.esperaSegundos = esperaSegundos;
    }

    public long getEsperaSegundos() { return esperaSegundos; }
}

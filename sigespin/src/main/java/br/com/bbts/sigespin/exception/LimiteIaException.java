package br.com.bbts.sigespin.exception;

public class LimiteIaException extends RuntimeException {
    private final long esperaSegundos;
    public LimiteIaException(long esperaSegundos) {
        super("Limite temporário da IA atingido.");
        this.esperaSegundos = esperaSegundos;
    }
    public long getEsperaSegundos() { return esperaSegundos; }
}

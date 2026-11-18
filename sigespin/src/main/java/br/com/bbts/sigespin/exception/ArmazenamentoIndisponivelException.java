package br.com.bbts.sigespin.exception;

public class ArmazenamentoIndisponivelException extends RuntimeException {
    public ArmazenamentoIndisponivelException(Throwable causa) { super("O armazenamento de arquivos está indisponível.", causa); }
}

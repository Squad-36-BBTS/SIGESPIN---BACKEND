package br.com.bbts.sigespin.service;

/** Uma operação válida em formato foi recusada por uma regra do domínio. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}

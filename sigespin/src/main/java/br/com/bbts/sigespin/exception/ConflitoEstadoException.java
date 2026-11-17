package br.com.bbts.sigespin.exception;

import br.com.bbts.sigespin.service.RegraNegocioException;

public class ConflitoEstadoException extends RegraNegocioException {
    public ConflitoEstadoException(String mensagem) {
        super(mensagem);
    }
}

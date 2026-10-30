package br.com.bbts.sigespin.service;

public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("Email ou senha inválidos.");
    }
}

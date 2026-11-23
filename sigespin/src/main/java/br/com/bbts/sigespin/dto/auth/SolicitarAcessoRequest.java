package br.com.bbts.sigespin.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido público de acesso. Não recebe senha nem perfil: quem cria o usuário decide. */
public record SolicitarAcessoRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 1000) String justificativa) {

    /**
     * Remove espaços nas pontas do e-mail antes da validação: e-mails copiados e colados costumam trazê-los,
     * e o @Email recusaria o pedido com 400. O serviço continua normalizando (strip + minúsculas).
     */
    public SolicitarAcessoRequest {
        email = email == null ? null : email.strip();
    }
}

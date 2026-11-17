package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.repository.seguranca.TokenJwtRevogadoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenJwtRevogacaoService {

    private final TokenJwtRevogadoRepository repository;

    public TokenJwtRevogacaoService(TokenJwtRevogadoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revogar(String jti, Instant expiraEm) {
        repository.inserirSeAusente(jti, expiraEm, Instant.now());
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void removerTokensExpirados() {
        repository.deleteByExpiraEmBefore(Instant.now());
    }
}

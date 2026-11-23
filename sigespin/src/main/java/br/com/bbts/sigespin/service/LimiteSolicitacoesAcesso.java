package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.exception.LimiteSolicitacoesAcessoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Limite global por minuto da rota pública de solicitação de acesso.
 * Fica em memória, por instância do backend, e zera quando o serviço reinicia.
 */
@Component
public class LimiteSolicitacoesAcesso {
    private final Clock relogio;
    private final int limitePorMinuto;
    private long minutoAtual = Long.MIN_VALUE;
    private int pedidosNoMinuto;

    @Autowired
    public LimiteSolicitacoesAcesso(@Value("${app.solicitacoes-acesso.limite-global-minuto:10}") int limitePorMinuto) {
        this(limitePorMinuto, Clock.systemUTC());
    }

    LimiteSolicitacoesAcesso(int limitePorMinuto, Clock relogio) {
        if (limitePorMinuto < 1) throw new IllegalArgumentException("O limite de solicitações deve ser positivo.");
        this.limitePorMinuto = limitePorMinuto;
        this.relogio = relogio;
    }

    public synchronized void consumir() {
        long segundo = relogio.instant().getEpochSecond();
        long minuto = segundo / 60;
        if (minuto != minutoAtual) {
            minutoAtual = minuto;
            pedidosNoMinuto = 0;
        }
        if (pedidosNoMinuto >= limitePorMinuto) {
            throw new LimiteSolicitacoesAcessoException(60 - Math.floorMod(segundo, 60));
        }
        pedidosNoMinuto++;
    }
}

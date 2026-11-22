package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.exception.LimiteSolicitacoesAcessoException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class LimiteSolicitacoesAcessoTests {
    static class Relogio extends Clock {
        Instant agora = Instant.parse("2026-11-22T12:00:15Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return agora; }
    }

    @Test void bloqueiaAcimaDoLimiteInformaEsperaERenovaNoMinutoSeguinte() {
        var relogio = new Relogio();
        var limite = new LimiteSolicitacoesAcesso(2, relogio);
        limite.consumir();
        limite.consumir();
        var erro = assertThrows(LimiteSolicitacoesAcessoException.class, limite::consumir);
        assertEquals(45, erro.getEsperaSegundos());
        relogio.agora = relogio.agora.plusSeconds(45);
        assertDoesNotThrow(limite::consumir);
    }

    @Test void rejeitaLimiteNaoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new LimiteSolicitacoesAcesso(0, new Relogio()));
    }
}

package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.exception.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ProtecaoIaTests {
    static class Relogio extends Clock {
        Instant agora = Instant.parse("2026-11-17T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return agora; }
    }
    @Test void limitaUsuarioSemBloquearOutroUsuarioERenovaJanela() {
        var relogio = new Relogio();
        var p = new ProtecaoIa(1, 10, relogio);
        assertEquals("ok", p.executar("ana", () -> "ok"));
        assertEquals(60, assertThrows(LimiteIaException.class, () -> p.executar("ana", () -> "indevido")).getEsperaSegundos());
        assertEquals("ok", p.executar("bia", () -> "ok"));
        relogio.agora = relogio.agora.plusSeconds(60);
        assertEquals("ok", p.executar("ana", () -> "ok"));
    }
    @Test void limiteGlobalImpedeMaisChamadasMesmoDeUsuariosDiferentes() {
        var p = new ProtecaoIa(10, 2, new Relogio());
        p.executar("a", () -> 1); p.executar("b", () -> 1);
        assertThrows(LimiteIaException.class, () -> p.executar("c", () -> 1));
    }
    @Test void circuitoAbreSemChamarProvedorERecuperaAposSondagem() {
        var p = new ProtecaoIa(30, 30, new Relogio());
        var chamadas = new AtomicInteger();
        for (int i=0;i<5;i++) assertThrows(IaIndisponivelException.class, () -> p.executar("a", () -> {
            chamadas.incrementAndGet(); throw new IaIndisponivelException("falha");
        }));
        assertEquals(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN, p.circuito().getState());
        assertThrows(IaIndisponivelException.class, () -> p.executar("a", chamadas::incrementAndGet));
        assertEquals(5, chamadas.get());
        p.circuito().transitionToHalfOpenState();
        assertEquals("ok", p.executar("a", () -> "ok"));
        assertEquals(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED, p.circuito().getState());
    }
}

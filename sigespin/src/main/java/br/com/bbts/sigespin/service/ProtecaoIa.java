package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.exception.IaIndisponivelException;
import br.com.bbts.sigespin.exception.LimiteIaException;
import io.github.resilience4j.circuitbreaker.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Proteção por instância do backend. Em múltiplas réplicas, usar limites compartilhados. */
@Component
public class ProtecaoIa {
    private record Janela(long minuto, int chamadas) { }
    private final Map<String, Janela> usuarios = new HashMap<>();
    private final Clock relogio;
    private final int limiteUsuario, limiteGlobal;
    private long minutoGlobal = Long.MIN_VALUE;
    private int chamadasGlobais;
    private final CircuitBreaker circuito = CircuitBreaker.of("gemini", CircuitBreakerConfig.custom()
            .failureRateThreshold(50).minimumNumberOfCalls(5).slidingWindowSize(10)
            .waitDurationInOpenState(Duration.ofSeconds(30)).permittedNumberOfCallsInHalfOpenState(1).build());

    @Autowired
    public ProtecaoIa(@Value("${app.ia.limite-usuario-minuto:10}") int usuario,
                      @Value("${app.ia.limite-global-minuto:60}") int global) {
        this(usuario, global, Clock.systemUTC());
    }

    ProtecaoIa(int usuario, int global, Clock relogio) {
        if (usuario < 1 || global < 1) throw new IllegalArgumentException("Limites da IA devem ser positivos.");
        this.limiteUsuario = usuario;
        this.limiteGlobal = global;
        this.relogio = relogio;
    }

    public <T> T executar(String usuario, Supplier<T> chamada) {
        consumir(usuario);
        try { return circuito.executeSupplier(chamada); }
        catch (CallNotPermittedException e) {
            throw new IaIndisponivelException("A IA está em recuperação. Preencha manualmente e tente novamente depois.");
        }
    }

    private synchronized void consumir(String usuario) {
        long segundo = relogio.instant().getEpochSecond();
        long minuto = segundo / 60;
        if (minutoGlobal != minuto) {
            minutoGlobal = minuto;
            chamadasGlobais = 0;
            usuarios.clear(); // limita memória aos consumidores da janela atual.
        }
        var janela = usuarios.getOrDefault(usuario, new Janela(minuto, 0));
        if (chamadasGlobais >= limiteGlobal || janela.chamadas() >= limiteUsuario) {
            throw new LimiteIaException(60 - Math.floorMod(segundo, 60));
        }
        chamadasGlobais++;
        usuarios.put(usuario, new Janela(minuto, janela.chamadas() + 1));
    }

    public CircuitBreaker circuito() { return circuito; }
}

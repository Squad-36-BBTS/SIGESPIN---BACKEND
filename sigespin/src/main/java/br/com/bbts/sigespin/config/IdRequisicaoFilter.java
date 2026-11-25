package br.com.bbts.sigespin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Dá a cada requisição um id (aceita o X-Request-Id do cliente quando é seguro), grava no MDC
 * para aparecer em todas as linhas de log, devolve no cabeçalho e registra uma linha de acesso.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdRequisicaoFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Request-Id";
    public static final String CHAVE_MDC = "requestId";
    private static final Pattern ID_SEGURO = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private static final Logger log = LoggerFactory.getLogger("sigespin.acesso");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebido = request.getHeader(CABECALHO);
        String id = recebido != null && ID_SEGURO.matcher(recebido).matches()
                ? recebido : UUID.randomUUID().toString();
        long inicio = System.nanoTime();
        MDC.put(CHAVE_MDC, id);
        response.setHeader(CABECALHO, id);
        try {
            chain.doFilter(request, response);
        } finally {
            if (!request.getRequestURI().startsWith("/actuator")) {
                log.info("{} {} -> {} em {} ms", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), (System.nanoTime() - inicio) / 1_000_000);
            }
            MDC.remove(CHAVE_MDC);
        }
    }
}

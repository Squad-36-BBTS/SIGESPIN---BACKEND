package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.dto.fila.MinhaFilaResponse;
import br.com.bbts.sigespin.service.MinhaFilaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minha fila (BE-12): tarefas pendentes do usuário atual, conforme o perfil. */
@RestController
@RequestMapping("/api/minha-fila")
public class MinhaFilaController {
    private final MinhaFilaService minhaFila;

    public MinhaFilaController(MinhaFilaService minhaFila) {
        this.minhaFila = minhaFila;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public MinhaFilaResponse obter() {
        return minhaFila.obter();
    }
}

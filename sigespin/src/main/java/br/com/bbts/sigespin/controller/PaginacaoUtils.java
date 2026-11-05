package br.com.bbts.sigespin.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginacaoUtils {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;
    public static final int TAMANHO_PADRAO_PAGINA = 20;

    private PaginacaoUtils() { }

    public static Pageable ajustarLimite(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, TAMANHO_PADRAO_PAGINA);
        }
        int pagina = Math.max(0, pageable.getPageNumber());
        int tamanho = pageable.getPageSize();
        if (tamanho > TAMANHO_MAXIMO_PAGINA) {
            tamanho = TAMANHO_MAXIMO_PAGINA;
        } else if (tamanho <= 0) {
            tamanho = TAMANHO_PADRAO_PAGINA;
        }
        return PageRequest.of(pagina, tamanho, pageable.getSort());
    }

    public static Pageable criar(int pagina, int tamanho, Sort sort) {
        int p = Math.max(0, pagina);
        int t = tamanho > TAMANHO_MAXIMO_PAGINA ? TAMANHO_MAXIMO_PAGINA : (tamanho <= 0 ? TAMANHO_PADRAO_PAGINA : tamanho);
        return PageRequest.of(p, t, sort != null ? sort : Sort.unsorted());
    }
}

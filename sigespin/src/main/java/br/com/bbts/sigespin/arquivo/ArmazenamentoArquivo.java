package br.com.bbts.sigespin.arquivo;

import org.springframework.core.io.Resource;

/** Cada chave identifica um objeto novo e imutável no armazenamento privado. */
public interface ArmazenamentoArquivo {
    String tipo();
    void guardar(String chave, byte[] conteudo, String contentType);
    Resource abrir(String chave);
    void remover(String chave);
}

package br.com.bbts.sigespin.arquivo;

import br.com.bbts.sigespin.exception.ArmazenamentoIndisponivelException;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;

@Component
public class ArmazenamentoLocal implements ArmazenamentoArquivo {
    private final Path diretorio;

    public ArmazenamentoLocal(@Value("${app.arquivos.diretorio:./arquivos}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public String tipo() { return "LOCAL"; }

    public void guardar(String chave, byte[] conteudo, String contentType) {
        try {
            Path destino = resolver(chave);
            Files.createDirectories(destino.getParent());
            if (!destino.getParent().toRealPath().startsWith(diretorio.toRealPath())) {
                throw new RecursoNaoEncontradoException("Arquivo indisponível.");
            }
            // CREATE_NEW impede a substituição de conteúdo de uma evidência anterior.
            Files.write(destino, conteudo, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) { throw new ArmazenamentoIndisponivelException(e); }
    }

    public Resource abrir(String chave) {
        try {
            Path arquivo = resolver(chave).toRealPath();
            if (!arquivo.startsWith(diretorio.toRealPath()) || !Files.isRegularFile(arquivo)) {
                throw new RecursoNaoEncontradoException("Arquivo indisponível.");
            }
            return new FileSystemResource(arquivo);
        } catch (NoSuchFileException e) {
            throw new RecursoNaoEncontradoException("Conteúdo do arquivo não encontrado.");
        } catch (IOException e) { throw new ArmazenamentoIndisponivelException(e); }
    }

    public void remover(String chave) {
        try {
            Path destino = resolver(chave);
            if (!Files.exists(destino, LinkOption.NOFOLLOW_LINKS)) return;
            if (!destino.getParent().toRealPath().startsWith(diretorio.toRealPath())) {
                throw new RecursoNaoEncontradoException("Arquivo indisponível.");
            }
            Files.deleteIfExists(destino);
        }
        catch (IOException e) { throw new ArmazenamentoIndisponivelException(e); }
    }

    private Path resolver(String chave) {
        Path destino = diretorio.resolve(chave).normalize();
        if (!destino.startsWith(diretorio) || destino.equals(diretorio)) {
            throw new RecursoNaoEncontradoException("Arquivo indisponível.");
        }
        return destino;
    }
}

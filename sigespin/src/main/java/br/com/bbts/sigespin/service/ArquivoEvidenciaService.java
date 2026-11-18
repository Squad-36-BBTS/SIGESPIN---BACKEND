package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.arquivo.*;
import br.com.bbts.sigespin.dto.entrega.EvidenciaResponse;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.model.entity.Evidencia;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.evidencia.EvidenciaRepository;
import br.com.bbts.sigespin.repository.portfolio.ProjetoRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArquivoEvidenciaService {
    private static final Logger log = LoggerFactory.getLogger(ArquivoEvidenciaService.class);
    private final ProjetoRepository projetos;
    private final EvidenciaRepository evidencias;
    private final UsuarioAtualService usuarios;
    private final ValidadorArquivo validador;
    private final Map<String, ArmazenamentoArquivo> armazenamentos;
    private final String tipo;

    public ArquivoEvidenciaService(ProjetoRepository projetos, EvidenciaRepository evidencias,
            UsuarioAtualService usuarios, ValidadorArquivo validador, List<ArmazenamentoArquivo> armazenamentos,
            @Value("${app.arquivos.tipo:LOCAL}") String tipo) {
        this.projetos = projetos;
        this.evidencias = evidencias;
        this.usuarios = usuarios;
        this.validador = validador;
        this.armazenamentos = armazenamentos.stream().collect(Collectors.toMap(ArmazenamentoArquivo::tipo, Function.identity()));
        this.tipo = tipo.toUpperCase(Locale.ROOT);
        if (!this.armazenamentos.containsKey(this.tipo)) throw new IllegalArgumentException("Tipo de armazenamento indisponível.");
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("isAuthenticated()")
    public EvidenciaResponse enviar(Long projetoId, MultipartFile arquivo) throws IOException {
        var autor = usuarios.obterAtivo();
        var projeto = projetos.findAtivoParaAtualizacao(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado ou arquivado."));
        if (autor.getPerfil() == PerfilUsuario.DEMANDANTE && (projeto.getDemandante() == null
                || !projeto.getDemandante().getId().equals(autor.getId()))) {
            throw new AccessDeniedException("Somente o Demandante responsável pode enviar comprovantes ao projeto.");
        }
        var validado = validador.validar(arquivo);
        var armazenamento = armazenamentos.get(tipo);
        String chave = "projetos/" + projetoId + "/" + UUID.randomUUID() + "." + validado.extensao();
        // A compensação remove o objeto recém-criado caso a transação de metadados seja desfeita.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { armazenamento.remover(chave); }
                    catch (RuntimeException e) { log.error("Falha ao remover objeto de upload revertido: {}", chave); }
                }
            }
        });
        armazenamento.guardar(chave, validado.conteudo(), validado.contentType());
        var evidencia = new Evidencia();
        evidencia.setProjeto(projeto);
        evidencia.setNomeArquivo(validado.nome());
        evidencia.setTipoArquivo(validado.contentType());
        evidencia.setCaminhoStorage(chave);
        evidencia.setTamanhoBytes((long) validado.conteudo().length);
        evidencia.setSha256(hash(validado.conteudo()));
        evidencia.setEnviadaPor(autor);
        evidencia.setArmazenamento(tipo);
        return EvidenciaResponse.de(evidencias.saveAndFlush(evidencia));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<EvidenciaResponse> listar(Long projetoId) {
        conferirProjeto(projetoId);
        return evidencias.findByProjetoIdOrderByDataUploadDesc(projetoId).stream().map(EvidenciaResponse::de).toList();
    }

    public record Conteudo(Resource recurso, String nome, String contentType, Long tamanho) { }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Conteudo baixar(Long projetoId, Long evidenciaId) {
        conferirProjeto(projetoId);
        var evidencia = evidencias.findById(evidenciaId)
                .filter(e -> e.getProjeto().getId().equals(projetoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evidência não encontrada no projeto."));
        String origem = evidencia.getArmazenamento() == null ? tipo : evidencia.getArmazenamento();
        var armazenamento = armazenamentos.get(origem);
        if (armazenamento == null) throw new RecursoNaoEncontradoException("Armazenamento do arquivo indisponível.");
        return new Conteudo(armazenamento.abrir(evidencia.getCaminhoStorage()), evidencia.getNomeArquivo(),
                evidencia.getTipoArquivo(), evidencia.getTamanhoBytes());
    }

    private void conferirProjeto(Long id) {
        usuarios.obterAtivo();
        if (!projetos.existsById(id)) throw new RecursoNaoEncontradoException("Projeto não encontrado.");
    }

    private String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}

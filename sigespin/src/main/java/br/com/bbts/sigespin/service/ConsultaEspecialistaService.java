package br.com.bbts.sigespin.service;

import br.com.bbts.sigespin.dto.consulta.*;
import br.com.bbts.sigespin.model.entity.ConsultaEspecialista;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.repository.ConsultaEspecialistaRepository;
import br.com.bbts.sigespin.repository.ProjetoRepository;
import br.com.bbts.sigespin.repository.UsuarioRepository;
import br.com.bbts.sigespin.security.UsuarioAtualService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
public class ConsultaEspecialistaService {
    private final ProjetoRepository projetos;
    private final UsuarioRepository usuarios;
    private final ConsultaEspecialistaRepository consultas;
    private final UsuarioAtualService usuarioAtual;

    public ConsultaEspecialistaService(ProjetoRepository projetos, UsuarioRepository usuarios,
            ConsultaEspecialistaRepository consultas, UsuarioAtualService usuarioAtual) {
        this.projetos = projetos;
        this.usuarios = usuarios;
        this.consultas = consultas;
        this.usuarioAtual = usuarioAtual;
    }

    /** Encaminha a questão sem criar parecer automático ou exigir qualificação prévia. */
    @Transactional
    @PreAuthorize("hasRole('ANALISTA')")
    public ConsultaRegistradaResponse consultar(@NotNull @Positive Long projetoId,
            @NotNull @Valid ConsultarEspecialistaRequest entrada) {
        var solicitante = usuarioAtual.exigirPerfil(PerfilUsuario.ANALISTA);
        var projeto = projetos.findById(projetoId)
                .orElseThrow(() -> new RegraNegocioException("Projeto não encontrado."));
        var especialista = usuarios.findById(entrada.especialistaId())
                .orElseThrow(() -> new RegraNegocioException("Especialista não encontrado."));
        if (!Boolean.TRUE.equals(especialista.getAtivo())
                || especialista.getPerfil() != PerfilUsuario.ESPECIALISTA) {
            throw new RegraNegocioException("O destinatário precisa ser um Especialista ativo.");
        }

        var consulta = new ConsultaEspecialista();
        consulta.setProjeto(projeto);
        consulta.setSolicitante(solicitante);
        consulta.setEspecialista(especialista);
        consulta.setQuestao(entrada.questao().strip());
        consulta.setCriadoEm(LocalDateTime.now());
        consultas.saveAndFlush(consulta);
        return new ConsultaRegistradaResponse(consulta.getId(), projeto.getId(), solicitante.getId(),
                especialista.getId(), consulta.getQuestao(), consulta.getCriadoEm());
    }
}

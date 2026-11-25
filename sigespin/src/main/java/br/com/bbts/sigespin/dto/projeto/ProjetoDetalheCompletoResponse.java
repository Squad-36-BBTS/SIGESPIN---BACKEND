package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.model.enums.ResultadoValidacao;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.model.enums.TipoAlteracaoProjeto;
import br.com.bbts.sigespin.model.enums.TipoLeituraRadar;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalhe do projeto numa única resposta, no formato {@code ProjetoDetalhe} do frontend: os campos de
 * {@link ProjetoResponse} no primeiro nível, seguidos dos registros do projeto em ordem cronológica.
 */
public record ProjetoDetalheCompletoResponse(
        @JsonUnwrapped ProjetoResponse projeto,
        Long acaoMobilizacaoId,
        Long arquivadoPorId,
        RecomendacaoOrigem recomendacao,
        List<Qualificacao> qualificacoes,
        List<Consulta> consultas,
        List<Parecer> pareceres,
        List<AvaliacaoTrl> avaliacoesTrl,
        List<AvaliacaoRadar> avaliacoesRadar,
        List<Solicitacao> solicitacoes,
        List<Entrega> entregas,
        List<Correcao> correcoes,
        List<Evidencia> evidencias,
        List<HistoricoTransicao> historicoTransicoes,
        List<HistoricoGestao> historicoGestao,
        List<Interacao> interacoes,
        List<TransicaoDisponivel> transicoesDisponiveis
) {
    public record RecomendacaoOrigem(Long id, String titulo) { }

    public record Qualificacao(Long id, Long projetoId, UsuarioResumoResponse analista, String aderenciaTese,
            String relevancia, String grauInovacao, String viabilidade, String maturidadeTecnologica,
            String fundamentacao, String conclusao, LocalDateTime criadaEm) { }

    /** {@code status}: PENDENTE ou RESPONDIDA. {@code areaTecnica} não é registrada no backend. */
    public record Consulta(Long id, Long projetoId, UsuarioResumoResponse solicitante,
            UsuarioResumoResponse especialista, String areaTecnica, String questao, String status, Long parecerId,
            LocalDateTime criadaEm, LocalDateTime respondidaEm) { }

    public record Parecer(Long id, Long projetoId, Long consultaId, String questao, UsuarioResumoResponse especialista,
            String fundamentacao, String conclusao, List<Evidencia> evidencias, LocalDateTime criadoEm) { }

    /** A leitura do radar é registrada à parte, por isso {@code radarId} vem sempre nulo. */
    public record AvaliacaoTrl(Long id, Long projetoId, UsuarioResumoResponse responsavel, Integer trlAnterior,
            Integer trlNovo, String justificativa, List<Evidencia> evidencias, Long radarId, LocalDateTime criadaEm) { }

    public record AvaliacaoRadar(Long id, Long projetoId, TipoLeituraRadar tipoLeitura, BigDecimal scoreNegocio,
            BigDecimal scoreProduto, BigDecimal scoreTime, BigDecimal scoreReceita, LocalDateTime dataAvaliacao) { }

    public record Solicitacao(Long id, Long projetoId, Long transicaoId, EtapaResumoResponse origem,
            EtapaResumoResponse destino, UsuarioResumoResponse solicitante, String justificativa,
            List<Evidencia> evidencias, StatusSolicitacao status, UsuarioResumoResponse encerradaPor,
            String justificativaEncerramento, LocalDateTime encerradaEm, LocalDateTime criadaEm) { }

    public record Entrega(Long id, Long projetoId, String descricao, UsuarioResumoResponse registradaPor,
            LocalDateTime registradaEm, List<Validacao> validacoes) { }

    public record Validacao(Long id, Long entregaId, ResultadoValidacao resultado, String observacoes,
            UsuarioResumoResponse por, LocalDateTime em) { }

    public record Correcao(Long id, Long projetoId, String descricao, String referencia, Evidencia evidencia,
            UsuarioResumoResponse autor, LocalDateTime criadaEm) { }

    /** {@code downloadUrl} já começa com /api: usar sem repetir o prefixo da baseURL. */
    public record Evidencia(Long id, Long projetoId, String nomeArquivo, String tipoArquivo, Long tamanhoBytes,
            LocalDateTime dataUpload, UsuarioResumoResponse enviadaPor, String estadoEnvio, String downloadUrl) { }

    /**
     * Mudanças de etapa (movimentações) e de TRL (avaliações) numa só linha do tempo. O {@code id} é único
     * dentro da lista: etapa usa o id da movimentação × 2, TRL o id da avaliação × 2 + 1.
     */
    public record HistoricoTransicao(Long id, Long projetoId, UsuarioResumoResponse usuario, String tipoMudanca,
            String valorAnterior, String valorNovo, String motivo, Long solicitacaoId, LocalDateTime dataMudanca) { }

    public record HistoricoGestao(Long id, Long projetoId, TipoAlteracaoProjeto tipo, UsuarioResumoResponse usuario,
            String justificativa, List<AlteracaoCampo> alteracoes, LocalDateTime data) { }

    public record AlteracaoCampo(String campo, String anterior, String atual) { }

    public record Interacao(Long id, Long projetoId, UsuarioResumoResponse autor, PerfilUsuario perfil, String texto,
            LocalDateTime criadaEm) { }

    public record TransicaoDisponivel(Long id, Long origemId, Long destinoId, boolean exigeGovernanca, boolean ativa,
            Long versao, EtapaResumoResponse destino) { }
}

package br.com.bbts.sigespin.dto.fila;

import br.com.bbts.sigespin.model.enums.ArenaTipo;
import br.com.bbts.sigespin.model.enums.EstadoTarefa;
import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import br.com.bbts.sigespin.model.enums.PerfilUsuario;
import br.com.bbts.sigespin.model.enums.ResultadoValidacao;
import br.com.bbts.sigespin.model.enums.StatusRecomendacao;
import br.com.bbts.sigespin.model.enums.StatusSolicitacao;
import br.com.bbts.sigespin.model.enums.TipoConfiguracaoKanban;
import br.com.bbts.sigespin.model.enums.TipoTerminal;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Minha fila (BE-12): tarefas do usuário atual, no mesmo formato do tipo {@code MinhaFila} do frontend.
 *
 * <p>São dez listas; cada perfil preenche apenas as suas e recebe as demais vazias. Os nomes dos campos
 * seguem os tipos do frontend ({@code criadaEm}, {@code projetoTitulo}, {@code registradaPor}...) para que a
 * tela use a resposta sem adaptação. Rotas antigas não mudam.</p>
 */
public record MinhaFilaResponse(
        List<ProjetoFila> triagem,
        List<ConsultaFila> consultasPendentes,
        List<SolicitacaoFila> solicitacoesPendentes,
        List<SolicitacaoFila> solicitacoesEncerradas,
        List<ParecerFila> pareceresRecebidos,
        List<PropostaFila> propostasPendentes,
        List<EntregaFila> entregasParaValidar,
        List<EntregaFila> entregasValidadas,
        List<ProjetoFila> minhasDemandas,
        List<RecomendacaoFila> recomendacoesPendentes) {

    public record UsuarioFila(Long id, String nome) { }

    /** {@code cor} não é guardada no banco: vem da cor padrão de cada etapa (ver {@code MinhaFilaService}). */
    public record EtapaFila(Long id, String codigo, String nome, String cor, boolean terminal) { }

    /** Ação de mobilização vinculada a uma demanda induzida. O banco não guarda o tipo: vai como OUTRA. */
    public record CampanhaFila(Long id, String titulo, String tipo) { }

    public record ProjetoFila(
            Long id,
            String titulo,
            String descricaoDor,
            OrigemDemanda origem,
            CampanhaFila campanha,
            Long areaDemandanteId,
            String areaDemandanteNome,
            UsuarioFila demandante,
            UsuarioFila analistaResponsavel,
            ArenaTipo arena,
            String hipoteseSolucao,
            Boolean potencialLeiDoBem,
            BigDecimal dispendioEstimado,
            EtapaFila etapa,
            Integer trlAtual,
            List<EstadoTarefa> pendencias,
            boolean arquivado,
            String motivoArquivamento,
            LocalDateTime arquivadoEm,
            Long versao,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm) { }

    public record EvidenciaFila(
            Long id,
            Long projetoId,
            String nomeArquivo,
            String tipoArquivo,
            Long tamanhoBytes,
            LocalDateTime dataUpload,
            UsuarioFila enviadaPor,
            String estadoEnvio) { }

    /** {@code status} é sempre PENDENTE aqui: a fila só mostra consultas ainda sem parecer. */
    public record ConsultaFila(
            Long id,
            Long projetoId,
            String projetoTitulo,
            UsuarioFila solicitante,
            UsuarioFila especialista,
            String areaTecnica,
            String questao,
            String status,
            Long parecerId,
            LocalDateTime criadaEm,
            LocalDateTime respondidaEm) { }

    public record SolicitacaoFila(
            Long id,
            Long projetoId,
            String projetoTitulo,
            Long transicaoId,
            EtapaFila origem,
            EtapaFila destino,
            UsuarioFila solicitante,
            String justificativa,
            List<EvidenciaFila> evidencias,
            StatusSolicitacao status,
            UsuarioFila encerradaPor,
            String justificativaEncerramento,
            LocalDateTime encerradaEm,
            LocalDateTime criadaEm) { }

    public record ParecerFila(
            Long id,
            Long projetoId,
            String projetoTitulo,
            Long consultaId,
            String questao,
            UsuarioFila especialista,
            String fundamentacao,
            String conclusao,
            List<EvidenciaFila> evidencias,
            LocalDateTime criadoEm) { }

    /** Proposta de nova coluna: só os campos que existem para esse tipo. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PropostaColuna(String codigo, String nome, Integer ordem, TipoTerminal terminal) { }

    public record PropostaTransicao(Long origemId, Long destinoId, Boolean exigeGovernanca) { }

    /** {@code proposta} é {@link PropostaColuna} ou {@link PropostaTransicao}, conforme {@code tipo}. */
    public record PropostaFila(
            Long id,
            TipoConfiguracaoKanban tipo,
            Object proposta,
            String justificativa,
            UsuarioFila autor,
            StatusSolicitacao status,
            UsuarioFila encerradaPor,
            String justificativaEncerramento,
            LocalDateTime encerradaEm,
            LocalDateTime criadaEm) { }

    public record ValidacaoFila(
            Long id,
            Long entregaId,
            ResultadoValidacao resultado,
            String observacoes,
            UsuarioFila por,
            LocalDateTime em) { }

    public record EntregaFila(
            Long id,
            Long projetoId,
            String projetoTitulo,
            String descricao,
            UsuarioFila registradaPor,
            LocalDateTime registradaEm,
            List<ValidacaoFila> validacoes) { }

    public record ProjetoReferencia(Long id, String titulo) { }

    public record RecomendacaoFila(
            Long id,
            UsuarioFila autor,
            PerfilUsuario autorPerfil,
            UsuarioFila destinatario,
            String titulo,
            String descricao,
            StatusRecomendacao status,
            ProjetoReferencia projeto,
            String justificativaEncerramento,
            LocalDateTime encerradaEm,
            LocalDateTime criadaEm) { }
}

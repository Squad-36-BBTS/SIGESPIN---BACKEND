package br.com.bbts.sigespin.model.enums;

/**
 * Pendência derivada de um projeto: o que está esperando alguém agir.
 * Não é gravada no banco; é calculada a partir dos registros do projeto ({@code PendenciaProjetoService}).
 */
public enum EstadoTarefa {
    /** Projeto ativo, fora de etapa terminal e ainda sem qualificação do Analista. */
    AGUARDANDO_TRIAGEM,
    /** Há consulta a Especialista sem parecer. */
    AGUARDANDO_PARECER,
    /** Há pedido de transição do Kanban aguardando decisão de governança. */
    AGUARDANDO_GOVERNANCA,
    /** Há entrega/MVP submetida e ainda não validada pelo Demandante. */
    AGUARDANDO_VALIDACAO
}

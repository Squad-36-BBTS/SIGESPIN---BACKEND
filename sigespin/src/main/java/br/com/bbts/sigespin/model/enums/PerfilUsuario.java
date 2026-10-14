package br.com.bbts.sigespin.model.enums;

/**
 * Cada perfil tem permissões diferentes na aplicação:
 * - DEMANDANTE: Pode criar projetos e solicitar análise da IA.
 * - ANALISTA:   Analisa projetos, anexa evidências e solicita avanços de TRL e etapa.
 * - COMITE:     Decide solicitações de avanço e registra deliberações.
 * - EXECUTIVO:  Perfil de visualização com acesso a dados financeiros.
 */

public enum PerfilUsuario {
    DEMANDANTE,
    ANALISTA,
    COMITE,
    EXECUTIVO
}

package br.com.bbts.sigespin.model.enums;

/**
 * Cada perfil tem permissões diferentes na aplicação:
 * - DEMANDANTE: Pode criar projetos e solicitar análise da IA.
 * - ANALISTA:   Pode criar projetos, solicitar IA e atualizar TRL.
 * - COMITE:     Pode deliberar, mudar status do funil e atualizar TRL.
 * - EXECUTIVO:  Perfil de visualização com acesso a dados financeiros.
 */

public enum PerfilUsuario {
    DEMANDANTE,
    ANALISTA,
    COMITE,
    EXECUTIVO
}

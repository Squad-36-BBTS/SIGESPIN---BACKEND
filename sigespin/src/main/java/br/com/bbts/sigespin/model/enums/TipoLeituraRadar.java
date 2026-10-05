package br.com.bbts.sigespin.model.enums;

/**
 * Tipo de leitura do Radar de Maturidade.
 * O Radar é uma avaliação com 4 dimensões (negócio, produto, time, receita).
 * - INICIAL: Gerada automaticamente quando o projeto é criado (vem da IA).
 * - ATUAL:   Registrada manualmente por ANALISTA ou COMITE ao longo do tempo.
 * Isso permite comparar a evolução do projeto: "como ele começou" vs "como está agora".
 */
public enum TipoLeituraRadar {
    INICIAL,
    ATUAL
}

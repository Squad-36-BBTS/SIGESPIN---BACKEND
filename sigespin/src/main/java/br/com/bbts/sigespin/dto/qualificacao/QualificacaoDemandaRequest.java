package br.com.bbts.sigespin.dto.qualificacao;

import jakarta.validation.constraints.NotBlank;

public record QualificacaoDemandaRequest(
        @NotBlank(message = "A análise de aderência à tese é obrigatória.")
        String aderenciaTese,

        @NotBlank(message = "A análise de relevância é obrigatória.")
        String relevancia,

        @NotBlank(message = "A análise do grau de inovação é obrigatória.")
        String grauInovacao,

        @NotBlank(message = "A análise de viabilidade é obrigatória.")
        String viabilidade,

        @NotBlank(message = "A análise de maturidade tecnológica é obrigatória.")
        String maturidadeTecnologica,

        @NotBlank(message = "A fundamentação da qualificação é obrigatória.")
        String fundamentacao,

        @NotBlank(message = "A conclusão da qualificação é obrigatória.")
        String conclusao
) { }

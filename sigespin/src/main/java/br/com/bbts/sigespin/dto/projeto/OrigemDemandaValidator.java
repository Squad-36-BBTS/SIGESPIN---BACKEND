package br.com.bbts.sigespin.dto.projeto;

import br.com.bbts.sigespin.model.enums.OrigemDemanda;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OrigemDemandaValidator implements ConstraintValidator<OrigemDemandaValida, DemandaCreateRequest> {

    @Override
    public boolean isValid(DemandaCreateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.origem() == null) {
            return true;
        }

        if (request.origem() == OrigemDemanda.INDUZIDA) {
            if (request.acaoMobilizacaoId() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Ação de mobilização é obrigatória para demandas induzidas.")
                        .addPropertyNode("acaoMobilizacaoId")
                        .addConstraintViolation();
                return false;
            }
        } else if (request.origem() == OrigemDemanda.ESPONTANEA) {
            if (request.acaoMobilizacaoId() != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Demandas espontâneas não devem possuir ação de mobilização vinculada.")
                        .addPropertyNode("acaoMobilizacaoId")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}

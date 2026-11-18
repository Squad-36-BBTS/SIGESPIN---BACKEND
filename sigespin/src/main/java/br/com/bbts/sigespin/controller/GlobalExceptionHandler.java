package br.com.bbts.sigespin.controller;

import br.com.bbts.sigespin.exception.ConflitoEstadoException;
import br.com.bbts.sigespin.exception.IaIndisponivelException;
import br.com.bbts.sigespin.exception.RecursoNaoEncontradoException;
import br.com.bbts.sigespin.exception.ServicoIndisponivelException;
import br.com.bbts.sigespin.security.ApiErroResponse;
import br.com.bbts.sigespin.service.RegraNegocioException;
import org.springframework.dao.CannotAcquireLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();

        if (detalhes.isEmpty()) {
            detalhes = ex.getBindingResult().getGlobalErrors().stream()
                    .map(erro -> erro.getObjectName() + ": " + erro.getDefaultMessage())
                    .toList();
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDACAO_FALHOU",
                "Revise os campos informados.",
                detalhes
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErroResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> detalhes = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDACAO_FALHOU",
                "Violação de validação nos parâmetros informados.",
                detalhes
        ));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiErroResponse> handleRegraNegocio(RegraNegocioException ex) {
        return ResponseEntity.unprocessableEntity().body(new ApiErroResponse(
                Instant.now(), 422, "REGRA_NEGOCIO", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ApiErroResponse> handleBloqueioConcorrente(CannotAcquireLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErroResponse(
                Instant.now(), 409, "CONFLITO_CONCORRENCIA",
                "Uma operação concorrente está atualizando o registro. Recarregue e tente novamente.", List.of()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "RECURSO_NAO_ENCONTRADO",
                ex.getMessage(),
                List.of()
        ));
    }

    @ExceptionHandler(ConflitoEstadoException.class)
    public ResponseEntity<ApiErroResponse> handleConflito(ConflitoEstadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "CONFLITO_ESTADO",
                ex.getMessage(),
                List.of()
        ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErroResponse> handleConcorrencia(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "CONFLITO_CONCORRENCIA",
                "O registro foi alterado por outra operação concorrente. Recarregue e tente novamente.",
                List.of()
        ));
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<ApiErroResponse> handleIaIndisponivel(IaIndisponivelException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "IA_INDISPONIVEL",
                "A sugestão da IA está indisponível. Preencha os campos manualmente.",
                List.of()
        ));
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ApiErroResponse> handleServicoIndisponivel(ServicoIndisponivelException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.NOT_IMPLEMENTED.value(),
                "SERVICO_NAO_DISPONIVEL",
                ex.getMessage(),
                List.of()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErroResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "ACESSO_NEGADO",
                "Seu perfil não tem permissão para esta ação.",
                List.of()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErroResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "NAO_AUTENTICADO",
                "Autenticação necessária ou sessão inválida.",
                List.of()
        ));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErroResponse> handleRequisicaoInvalida(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "REQUISICAO_INVALIDA",
                "Parâmetros inválidos ou corpo da requisição malformado.",
                List.of()
        ));
    }

    @ExceptionHandler(br.com.bbts.sigespin.exception.ArquivoInvalidoException.class)
    public ResponseEntity<ApiErroResponse> arquivoInvalido(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiErroResponse(Instant.now(), 400,
                "ARQUIVO_INVALIDO", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ApiErroResponse> arquivoAusente(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiErroResponse(Instant.now(), 400,
                "ARQUIVO_AUSENTE", "Envie o arquivo no campo 'arquivo'.", List.of()));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErroResponse> arquivoGrande(Exception ex) {
        return ResponseEntity.status(413).body(new ApiErroResponse(Instant.now(), 413,
                "ARQUIVO_MUITO_GRANDE", "O limite é de 10 MB por arquivo.", List.of()));
    }

    @ExceptionHandler(br.com.bbts.sigespin.exception.ArmazenamentoIndisponivelException.class)
    public ResponseEntity<ApiErroResponse> armazenamentoIndisponivel(Exception ex) {
        return ResponseEntity.status(503).body(new ApiErroResponse(Instant.now(), 503,
                "ARMAZENAMENTO_INDISPONIVEL", "Não foi possível acessar o arquivo. Tente novamente.", List.of()));
    }

    @ExceptionHandler(br.com.bbts.sigespin.exception.LimiteIaException.class)
    public ResponseEntity<ApiErroResponse> limiteIa(br.com.bbts.sigespin.exception.LimiteIaException ex) {
        return ResponseEntity.status(429).header("Retry-After", Long.toString(ex.getEsperaSegundos()))
                .body(new ApiErroResponse(Instant.now(), 429, "LIMITE_IA",
                        "Limite temporário da IA atingido. Aguarde para tentar novamente.", List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErroResponse> handleErroGeral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErroResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "ERRO_INTERNO",
                "Ocorreu um erro interno inesperado no servidor.",
                List.of()
        ));
    }
}

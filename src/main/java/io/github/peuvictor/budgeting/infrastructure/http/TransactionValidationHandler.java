package io.github.peuvictor.budgeting.infrastructure.http;

import io.github.peuvictor.budgeting.domain.InvalidTransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = TransactionController.class)
public class TransactionValidationHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleRequestValidation(
            MethodArgumentNotValidException exception
    ) {
        List<Map<String, String>> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null
                                ? "valor inválido"
                                : error.getDefaultMessage()
                ))
                .toList();

        return badRequest(errors);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(
            InvalidTransactionException exception
    ) {
        return badRequest(List.of(Map.of(
                "field", exception.getField(),
                "message", exception.getMessage()
        )));
    }

    private ResponseEntity<ProblemDetail> badRequest(
            List<Map<String, String>> errors
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Dados da transação inválidos"
        );
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}

package io.github.peuvictor.budgeting.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class AudioUploadExceptionHandler {

    @ExceptionHandler(InvalidAudioUploadException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAudio(InvalidAudioUploadException exception) {
        return problem(exception.getStatus(), "file", exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ProblemDetail> handleMissingFile(MissingServletRequestPartException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getRequestPartName(), "O arquivo é obrigatório");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadSize(MaxUploadSizeExceededException exception) {
        return problem(
                HttpStatus.CONTENT_TOO_LARGE, "file",
                "O arquivo ou a requisição excede o tamanho máximo permitido"
        );
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String field, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Arquivo de áudio inválido");
        problem.setProperty("errors", List.of(Map.of("field", field, "message", message)));
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}

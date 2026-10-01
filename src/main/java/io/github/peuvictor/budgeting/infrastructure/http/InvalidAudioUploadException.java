package io.github.peuvictor.budgeting.infrastructure.http;

import org.springframework.http.HttpStatus;

public class InvalidAudioUploadException extends RuntimeException {

    private final HttpStatus status;

    public InvalidAudioUploadException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

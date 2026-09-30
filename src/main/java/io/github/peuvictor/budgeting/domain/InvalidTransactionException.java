package io.github.peuvictor.budgeting.domain;

public class InvalidTransactionException extends IllegalArgumentException {

    private final String field;

    public InvalidTransactionException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

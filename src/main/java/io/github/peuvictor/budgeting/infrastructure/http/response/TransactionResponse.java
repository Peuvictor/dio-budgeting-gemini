package io.github.peuvictor.budgeting.infrastructure.http.response;

import io.github.peuvictor.budgeting.application.output.TransactionOutput;

public record TransactionResponse(
        String id,
        String category,
        String description,
        double amount
) {

    public static TransactionResponse from(TransactionOutput output) {
        return new TransactionResponse(
                output.id(),
                output.category(),
                output.description(),
                output.value()
        );
    }
}
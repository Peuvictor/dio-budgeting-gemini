package io.github.peuvictor.budgeting.infrastructure.http.request;

import io.github.peuvictor.budgeting.application.input.PersistTransactionInput;
import io.github.peuvictor.budgeting.domain.Category;

public record TransactionRequest(
        String description,
        Category category,
        long amount
) {

    public PersistTransactionInput toInput() {
        return new PersistTransactionInput(
                description,
                amount,
                category
        );
    }
}
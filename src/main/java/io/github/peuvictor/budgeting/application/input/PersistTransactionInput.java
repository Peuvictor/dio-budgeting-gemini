package io.github.peuvictor.budgeting.application.input;

import io.github.peuvictor.budgeting.domain.Category;

public record PersistTransactionInput(
        String description,
        long amount,
        Category category
) {
}
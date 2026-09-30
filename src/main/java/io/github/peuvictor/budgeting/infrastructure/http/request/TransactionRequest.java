package io.github.peuvictor.budgeting.infrastructure.http.request;

import io.github.peuvictor.budgeting.application.input.PersistTransactionInput;
import io.github.peuvictor.budgeting.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransactionRequest(
        @NotBlank(message = "é obrigatória")
        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String description,
        @NotNull(message = "é obrigatória")
        Category category,
        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser maior que zero")
        Long amount
) {

    public PersistTransactionInput toInput() {
        return new PersistTransactionInput(
                description,
                amount,
                category
        );
    }
}
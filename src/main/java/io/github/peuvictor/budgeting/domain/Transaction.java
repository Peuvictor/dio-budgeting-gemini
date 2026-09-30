package io.github.peuvictor.budgeting.domain;

import lombok.Getter;

@Getter
public class Transaction {

    private final TransactionId id;
    private final String description;
    private final long amount;
    private final Category category;

    public Transaction(
            String description,
            long amount,
            Category category
    ) {
        this(new TransactionId(), description, amount, category);
    }

    public Transaction(
            TransactionId id,
            String description,
            long amount,
            Category category
    ) {
        if (id == null || id.uuid() == null) {
            throw new InvalidTransactionException("id", "é obrigatório");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidTransactionException("description", "é obrigatória");
        }
        if (description.length() > 255) {
            throw new InvalidTransactionException("description", "deve ter no máximo 255 caracteres");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException("amount", "deve ser maior que zero");
        }
        if (category == null) {
            throw new InvalidTransactionException("category", "é obrigatória");
        }

        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
    }
}
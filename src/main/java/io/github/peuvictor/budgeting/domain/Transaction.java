package io.github.peuvictor.budgeting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
        this.id = new TransactionId();
        this.description = description;
        this.amount = amount;
        this.category = category;
    }
}
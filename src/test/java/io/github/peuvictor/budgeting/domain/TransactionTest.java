package io.github.peuvictor.budgeting.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void acceptsValidTransaction() {
        Transaction transaction = new Transaction("Mercado", 12533, Category.GROCERIES);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getDescription()).isEqualTo("Mercado");
        assertThat(transaction.getAmount()).isEqualTo(12533);
        assertThat(transaction.getCategory()).isEqualTo(Category.GROCERIES);
    }

    @Test
    void rejectsInvalidDescription() {
        assertInvalid("description", () -> new Transaction(null, 100, Category.OTHER));
        assertInvalid("description", () -> new Transaction("  ", 100, Category.OTHER));
        assertInvalid("description", () -> new Transaction("x".repeat(256), 100, Category.OTHER));
    }

    @Test
    void rejectsInvalidAmount() {
        assertInvalid("amount", () -> new Transaction("Mercado", 0, Category.GROCERIES));
        assertInvalid("amount", () -> new Transaction("Mercado", -100, Category.GROCERIES));
    }

    @Test
    void rejectsMissingCategory() {
        assertInvalid("category", () -> new Transaction("Mercado", 100, null));
    }

    @Test
    void validatesTransactionsLoadedFromPersistence() {
        assertInvalid("amount", () -> new Transaction(new TransactionId(), "Mercado", 0, Category.GROCERIES));
    }

    private void assertInvalid(String field, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(InvalidTransactionException.class)
                .satisfies(exception -> assertThat(((InvalidTransactionException) exception).getField())
                        .isEqualTo(field));
    }
}

package io.github.peuvictor.budgeting.application;

import io.github.peuvictor.budgeting.application.input.PersistTransactionInput;
import io.github.peuvictor.budgeting.domain.Category;
import io.github.peuvictor.budgeting.domain.InvalidTransactionException;
import io.github.peuvictor.budgeting.domain.Transaction;
import io.github.peuvictor.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistTransactionUseCaseTest {

    @Test
    void invalidToolInputNeverReachesRepository() {
        RecordingRepository repository = new RecordingRepository();
        PersistTransactionUseCase useCase = new PersistTransactionUseCase(repository);

        assertThatThrownBy(() -> useCase.execute(
                new PersistTransactionInput("Mercado", -100, Category.GROCERIES)
        )).isInstanceOf(InvalidTransactionException.class);

        assertThat(repository.saveCalled).isFalse();
    }

    private static class RecordingRepository implements TransactionRepository {
        private boolean saveCalled;

        @Override
        public Transaction save(Transaction transaction) {
            saveCalled = true;
            return transaction;
        }

        @Override
        public List<Transaction> findAllByCategory(Category category) {
            return List.of();
        }
    }
}

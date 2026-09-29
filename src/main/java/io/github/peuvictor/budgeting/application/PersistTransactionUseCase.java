package io.github.peuvictor.budgeting.application;

import io.github.peuvictor.budgeting.application.input.PersistTransactionInput;
import io.github.peuvictor.budgeting.application.output.TransactionOutput;
import io.github.peuvictor.budgeting.domain.Transaction;
import io.github.peuvictor.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class PersistTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public PersistTransactionUseCase(
            TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(
            name = "persist-transaction",
            description = "Persiste uma nova transação financeira"
    )
    public TransactionOutput execute(PersistTransactionInput input) {
        Transaction transaction = new Transaction(
                input.description(),
                input.amount(),
                input.category()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionOutput.from(savedTransaction);
    }
}
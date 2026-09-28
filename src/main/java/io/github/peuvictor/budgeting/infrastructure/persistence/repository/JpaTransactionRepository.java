package io.github.peuvictor.budgeting.infrastructure.persistence.repository;

import io.github.peuvictor.budgeting.domain.Category;
import io.github.peuvictor.budgeting.domain.Transaction;
import io.github.peuvictor.budgeting.domain.TransactionRepository;
import io.github.peuvictor.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaTransactionRepository implements TransactionRepository {

    private final TransactionEntityRepository transactionEntityRepository;

    public JpaTransactionRepository(
            TransactionEntityRepository transactionEntityRepository
    ) {
        this.transactionEntityRepository = transactionEntityRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity =
                TransactionEntity.from(transaction);

        TransactionEntity savedEntity =
                transactionEntityRepository.save(entity);

        return savedEntity.toDomain();
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactionEntityRepository
                .findAllByCategory(category)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }
}
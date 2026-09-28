package io.github.peuvictor.budgeting.infrastructure.persistence.repository;

import io.github.peuvictor.budgeting.domain.Category;
import io.github.peuvictor.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionEntityRepository
        extends CrudRepository<TransactionEntity, String> {

    List<TransactionEntity> findAllByCategory(Category category);
}
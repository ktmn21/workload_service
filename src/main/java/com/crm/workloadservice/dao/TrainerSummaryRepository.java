package com.crm.workloadservice.dao;

import com.crm.workloadservice.model.TrainerSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainerSummaryRepository
        extends MongoRepository<TrainerSummary, String>, TrainerSummaryRepositoryCustom {

    Optional<TrainerSummary> findByUsername(String username);

    boolean existsByUsername(String username);
}

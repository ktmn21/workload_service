package com.crm.workloadservice.dao;

import com.crm.workloadservice.model.TrainerSummary;

import java.util.Optional;

public interface TrainerSummaryRepositoryCustom {

    Optional<TrainerSummary> searchByUsername(String username);

    TrainerSummary updateByUsername(String username, TrainerSummary updated);
}

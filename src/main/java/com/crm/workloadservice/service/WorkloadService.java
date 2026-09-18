package com.crm.workloadservice.service;

import com.crm.workloadservice.dao.TrainerSummaryRepository;
import com.crm.workloadservice.dto.TrainerSummaryResponse;
import com.crm.workloadservice.dto.TrainerWorkloadRequest;
import com.crm.workloadservice.exception.TrainerNotFoundException;
import com.crm.workloadservice.model.TrainerSummary;
import com.crm.workloadservice.model.TrainingMonth;
import com.crm.workloadservice.model.TrainingYear;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private static final Logger txLog = LoggerFactory.getLogger("TRANSACTION");
    private static final Logger opLog = LoggerFactory.getLogger(WorkloadService.class);
    private static final String TX_KEY = "transactionId";

    private final TrainerSummaryRepository trainerSummaryRepository;

    public void process(TrainerWorkloadRequest request) {
        String txId = MDC.get(TX_KEY);
        txLog.info("[txId={}] START processWorkload username={} actionType={} trainingDate={} duration={}",
                txId, request.getUsername(), request.getActionType(),
                request.getTrainingDate(), request.getTrainingDuration());

        validate(request);

        try {
            switch (request.getActionType()) {
                case ADD -> handleAdd(request);
                case DELETE -> handleDelete(request);
            }
            txLog.info("[txId={}] END processWorkload username={} status=SUCCESS", txId, request.getUsername());
        } catch (RuntimeException ex) {
            txLog.error("[txId={}] END processWorkload username={} status=FAILED reason={}",
                    txId, request.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    public TrainerSummaryResponse getSummary(String username) {
        String txId = MDC.get(TX_KEY);
        opLog.info("[txId={}] operation=GET_SUMMARY username={}", txId, username);

        TrainerSummary trainer = trainerSummaryRepository.searchByUsername(username)
                .orElseThrow(() -> new TrainerNotFoundException("Trainer not found: " + username));

        List<TrainerSummaryResponse.YearSummary> years = trainer.getYears().stream()
                .map(y -> new TrainerSummaryResponse.YearSummary(
                        y.getYear(),
                        y.getMonths().stream()
                                .map(m -> new TrainerSummaryResponse.MonthSummary(
                                        m.getMonth(), m.getTrainingsSummaryDuration()))
                                .toList()))
                .toList();

        return new TrainerSummaryResponse(
                trainer.getUsername(), trainer.getFirstName(),
                trainer.getLastName(), trainer.getStatus(), years);
    }

    private void handleAdd(TrainerWorkloadRequest request) {
        String txId = MDC.get(TX_KEY);
        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();

        opLog.info("[txId={}] operation=SEARCH_TRAINER username={}", txId, request.getUsername());
        Optional<TrainerSummary> existing = trainerSummaryRepository.searchByUsername(request.getUsername());

        if (existing.isEmpty()) {
            opLog.info("[txId={}] operation=CREATE_TRAINER username={} year={} month={} duration={}",
                    txId, request.getUsername(), year, month, request.getTrainingDuration());

            TrainerSummary trainer = new TrainerSummary();
            trainer.setUsername(request.getUsername());
            trainer.setFirstName(request.getFirstName());
            trainer.setLastName(request.getLastName());
            trainer.setStatus(request.getIsActive());

            TrainingMonth trainingMonth = new TrainingMonth(month, request.getTrainingDuration());
            TrainingYear trainingYear = new TrainingYear(year, new ArrayList<>(List.of(trainingMonth)));
            trainer.addYear(trainingYear);

            trainerSummaryRepository.save(trainer);
            opLog.info("[txId={}] operation=SAVE_TRAINER username={} status=CREATED", txId, request.getUsername());
            return;
        }

        TrainerSummary trainer = existing.get();
        TrainingYear trainingYear = findOrCreateYear(trainer, year);
        TrainingMonth trainingMonth = findOrCreateMonth(trainingYear, month);

        int currentDuration = trainingMonth.getTrainingsSummaryDuration() == null
                ? 0 : trainingMonth.getTrainingsSummaryDuration();
        int updatedDuration = currentDuration + request.getTrainingDuration();
        trainingMonth.setTrainingsSummaryDuration(updatedDuration);

        opLog.info("[txId={}] operation=UPDATE_DURATION username={} year={} month={} oldValue={} addedValue={} newValue={}",
                txId, request.getUsername(), year, month, currentDuration, request.getTrainingDuration(), updatedDuration);

        trainerSummaryRepository.updateByUsername(request.getUsername(), trainer);
        opLog.info("[txId={}] operation=SAVE_TRAINER username={} status=UPDATED", txId, request.getUsername());
    }

    private void handleDelete(TrainerWorkloadRequest request) {
        String txId = MDC.get(TX_KEY);
        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();

        TrainerSummary trainer = trainerSummaryRepository.searchByUsername(request.getUsername())
                .orElseThrow(() -> new TrainerNotFoundException("Trainer not found: " + request.getUsername()));

        TrainingMonth trainingMonth = findMonth(trainer, year, month)
                .orElseThrow(() -> new TrainerNotFoundException(
                        "No training recorded for " + year + "-" + month + " for trainer " + request.getUsername()));

        int current = trainingMonth.getTrainingsSummaryDuration() == null ? 0 : trainingMonth.getTrainingsSummaryDuration();
        int remaining = current - request.getTrainingDuration();
        if (remaining < 0) {
            throw new IllegalArgumentException(
                    "Cannot subtract " + request.getTrainingDuration() + " minutes; only " + current + " recorded");
        }
        trainingMonth.setTrainingsSummaryDuration(remaining);

        opLog.info("[txId={}] operation=UPDATE_DURATION(DELETE) username={} year={} month={} oldValue={} subtractedValue={} newValue={}",
                txId, request.getUsername(), year, month, current, request.getTrainingDuration(), remaining);

        trainerSummaryRepository.updateByUsername(request.getUsername(), trainer);
        opLog.info("[txId={}] operation=SAVE_TRAINER username={} status=UPDATED", txId, request.getUsername());
    }

    private TrainingYear findOrCreateYear(TrainerSummary trainer, int year) {
        return trainer.getYears().stream()
                .filter(y -> y.getYear() != null && y.getYear() == year)
                .findFirst()
                .orElseGet(() -> {
                    TrainingYear newYear = new TrainingYear(year, new ArrayList<>());
                    trainer.addYear(newYear);
                    return newYear;
                });
    }

    private TrainingMonth findOrCreateMonth(TrainingYear trainingYear, int month) {
        return trainingYear.getMonths().stream()
                .filter(m -> m.getMonth() != null && m.getMonth() == month)
                .findFirst()
                .orElseGet(() -> {
                    TrainingMonth newMonth = new TrainingMonth(month, 0);
                    trainingYear.getMonths().add(newMonth);
                    return newMonth;
                });
    }

    private Optional<TrainingMonth> findMonth(TrainerSummary trainer, int year, int month) {
        return trainer.getYears().stream()
                .filter(y -> y.getYear() != null && y.getYear() == year)
                .findFirst()
                .flatMap(y -> y.getMonths().stream()
                        .filter(m -> m.getMonth() != null && m.getMonth() == month)
                        .findFirst());
    }

    private void validate(TrainerWorkloadRequest request) {
        if (request.getTrainingDuration() == null || request.getTrainingDuration() <= 0) {
            throw new IllegalArgumentException("Training duration must be a positive number");
        }
        if (request.getTrainingDate() == null) {
            throw new IllegalArgumentException("Training date must not be null");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
    }
}

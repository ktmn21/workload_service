package com.crm.workloadservice.service;

import com.crm.workloadservice.dao.TrainerSummaryRepository;
import com.crm.workloadservice.dto.ActionType;
import com.crm.workloadservice.dto.TrainerSummaryResponse;
import com.crm.workloadservice.dto.TrainerWorkloadRequest;
import com.crm.workloadservice.model.TrainerSummary;
import com.crm.workloadservice.model.TrainingMonth;
import com.crm.workloadservice.model.TrainingYear;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkloadService {

    private final TrainerSummaryRepository trainerSummaryRepository;

    public void process(TrainerWorkloadRequest request) {
        switch (request.getActionType()) {
            case ADD -> handleAdd(request);
            case DELETE -> handleDelete(request);
        }
    }

    @Transactional(readOnly = true)
    public TrainerSummaryResponse getSummary(String username) {
        TrainerSummary trainer = trainerSummaryRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found: " + username));

        List<TrainerSummaryResponse.YearSummary> years = trainer.getYearList().stream()
                .map(y -> new TrainerSummaryResponse.YearSummary(
                        y.getYear(),
                        y.getMonthList().stream()
                                .map(m -> new TrainerSummaryResponse.MonthSummary(m.getMonth(), m.getDuration()))
                                .toList()))
                .toList();

        return new TrainerSummaryResponse(
                trainer.getUsername(), trainer.getFirstName(),
                trainer.getLastName(), trainer.getStatus(), years);
    }

    private void handleAdd(TrainerWorkloadRequest request) {
        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();

        TrainerSummary trainer = getOrCreateTrainer(request);
        TrainingYear trainingYear = getOrCreateYear(trainer, year);
        TrainingMonth trainingMonth = getOrCreateMonth(trainingYear, month);

        trainingMonth.setDuration(trainingMonth.getDuration() + request.getTrainingDuration());
        trainerSummaryRepository.save(trainer);
    }

    private void handleDelete(TrainerWorkloadRequest request) {
        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();

        TrainerSummary trainer = trainerSummaryRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainer not found: " + request.getUsername()));

        TrainingMonth trainingMonth = findMonth(trainer, year, month)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No training recorded for " + year + "-" + month
                                + " for trainer " + request.getUsername()));

        int remaining = trainingMonth.getDuration() - request.getTrainingDuration();
        if (remaining < 0) {
            throw new IllegalArgumentException(
                    "Cannot delete " + request.getTrainingDuration()
                            + " hours; only " + trainingMonth.getDuration() + " recorded");
        }

        if (remaining == 0) {
            trainingMonth.getTrainingYear().getMonthList().remove(trainingMonth);
        } else {
            trainingMonth.setDuration(remaining);
        }
        trainerSummaryRepository.save(trainer);
    }

    private TrainerSummary getOrCreateTrainer(TrainerWorkloadRequest request) {
        return trainerSummaryRepository.findByUsername(request.getUsername())
                .orElseGet(() -> {
                    TrainerSummary summary = new TrainerSummary();
                    summary.setUsername(request.getUsername());
                    summary.setFirstName(request.getFirstName());
                    summary.setLastName(request.getLastName());
                    summary.setStatus(request.getIsActive());
                    return summary;
                });
    }

    private TrainingYear getOrCreateYear(TrainerSummary trainer, int year) {
        return trainer.getYearList().stream()
                .filter(y -> y.getYear().equals(year))
                .findFirst()
                .orElseGet(() -> {
                    TrainingYear newYear = new TrainingYear();
                    newYear.setYear(year);
                    trainer.addYear(newYear);
                    return newYear;
                });
    }

    private TrainingMonth getOrCreateMonth(TrainingYear trainingYear, int month) {
        return trainingYear.getMonthList().stream()
                .filter(m -> m.getMonth().equals(month))
                .findFirst()
                .orElseGet(() -> {
                    TrainingMonth newMonth = new TrainingMonth();
                    newMonth.setMonth(month);
                    newMonth.setDuration(0);
                    trainingYear.addMonth(newMonth);
                    return newMonth;
                });
    }

    private java.util.Optional<TrainingMonth> findMonth(TrainerSummary trainer, int year, int month) {
        return trainer.getYearList().stream()
                .filter(y -> y.getYear().equals(year))
                .findFirst()
                .flatMap(y -> y.getMonthList().stream()
                        .filter(m -> m.getMonth().equals(month))
                        .findFirst());
    }
}
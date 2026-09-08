package com.crm.workloadservice;

import com.crm.workloadservice.dao.TrainerSummaryRepository;
import com.crm.workloadservice.dto.ActionType;
import com.crm.workloadservice.dto.TrainerWorkloadRequest;
import com.crm.workloadservice.model.TrainerSummary;
import com.crm.workloadservice.model.TrainingMonth;
import com.crm.workloadservice.model.TrainingYear;
import com.crm.workloadservice.service.WorkloadService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {

    @Mock
    TrainerSummaryRepository trainerSummaryRepository;

    @InjectMocks
    WorkloadService service;

    private static final String USERNAME = "jane.smith";
    private static final LocalDate TRAINING_DATE = LocalDate.of(2026, 5, 15);

    private TrainerWorkloadRequest buildRequest(int duration, ActionType actionType) {
        return new TrainerWorkloadRequest(
                USERNAME,
                "Jane",
                "Smith",
                true,
                TRAINING_DATE,
                duration,
                actionType
        );
    }

    private TrainerSummary trainerWithMonth(int year, int month, int duration) {
        TrainerSummary trainer = new TrainerSummary();
        trainer.setUsername(USERNAME);
        trainer.setFirstName("Jane");
        trainer.setLastName("Smith");
        trainer.setStatus(true);

        TrainingYear trainingYear = new TrainingYear();
        trainingYear.setYear(year);
        trainer.addYear(trainingYear);

        TrainingMonth trainingMonth = new TrainingMonth();
        trainingMonth.setMonth(month);
        trainingMonth.setDuration(duration);
        trainingYear.addMonth(trainingMonth);

        return trainer;
    }

    @Test
    void handleAdd_newTrainer_createsWithDuration() {
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        TrainerWorkloadRequest request = buildRequest(10, ActionType.ADD);
        service.process(request);

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).save(captor.capture());

        TrainerSummary saved = captor.getValue();
        assertEquals(USERNAME, saved.getUsername());
        assertEquals("Jane", saved.getFirstName());
        assertEquals("Smith", saved.getLastName());
        assertEquals(true, saved.getStatus());
        assertThat(saved.getYearList()).hasSize(1);

        TrainingYear year = saved.getYearList().get(0);
        assertEquals(2026, year.getYear());
        assertThat(year.getMonthList()).hasSize(1);

        TrainingMonth month = year.getMonthList().get(0);
        assertEquals(5, month.getMonth());
        assertEquals(10, month.getDuration());
    }

    @Test
    void handleAdd_existingMonth_accumulatesDuration() {
        TrainerSummary existing = trainerWithMonth(2026, 5, 8);
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        TrainerWorkloadRequest request = buildRequest(10, ActionType.ADD);
        service.process(request);

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).save(captor.capture());

        TrainingMonth updatedMonth = captor.getValue().getYearList().get(0).getMonthList().get(0);
        assertEquals(18, updatedMonth.getDuration());
        // no duplicate month/year rows created
        assertThat(captor.getValue().getYearList()).hasSize(1);
        assertThat(captor.getValue().getYearList().get(0).getMonthList()).hasSize(1);
    }

    @Test
    void handleAdd_existingTrainerNewMonth_addsSeparateMonthEntry() {
        TrainerSummary existing = trainerWithMonth(2026, 4, 6);
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        TrainerWorkloadRequest request = buildRequest(10, ActionType.ADD);
        service.process(request);

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).save(captor.capture());

        TrainingYear year = captor.getValue().getYearList().get(0);
        assertThat(year.getMonthList()).hasSize(2);
        assertThat(year.getMonthList())
                .anySatisfy(m -> assertEquals(6, m.getDuration()))
                .anySatisfy(m -> assertEquals(10, m.getDuration()));
    }

    @Test
    void handleDelete_unknownTrainer_throwsNotFound() {
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        TrainerWorkloadRequest request = buildRequest(5, ActionType.DELETE);

        assertThrows(EntityNotFoundException.class, () -> service.process(request));
        verify(trainerSummaryRepository, never()).save(any());
    }

    @Test
    void handleDelete_noMatchingMonth_throwsNotFound() {
        TrainerSummary existing = new TrainerSummary();
        existing.setUsername(USERNAME);
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        TrainerWorkloadRequest request = buildRequest(5, ActionType.DELETE);

        assertThrows(EntityNotFoundException.class, () -> service.process(request));
        verify(trainerSummaryRepository, never()).save(any());
    }

    @Test
    void handleDelete_partialAmount_reducesDuration() {
        TrainerSummary existing = trainerWithMonth(2026, 5, 20);
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        TrainerWorkloadRequest request = buildRequest(8, ActionType.DELETE);
        service.process(request);

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).save(captor.capture());

        TrainingMonth updatedMonth = captor.getValue().getYearList().get(0).getMonthList().get(0);
        assertEquals(12, updatedMonth.getDuration());
    }

    @Test
    void handleDelete_moreThanExists_throwsIllegalArgument() {
        TrainerSummary existing = trainerWithMonth(2026, 5, 5);
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        TrainerWorkloadRequest request = buildRequest(10, ActionType.DELETE); // 10 > 5

        assertThrows(IllegalArgumentException.class, () -> service.process(request));
        verify(trainerSummaryRepository, never()).save(any());

        assertEquals(5, existing.getYearList().get(0).getMonthList().get(0).getDuration());
    }

    @Test
    void handleDelete_exactAmount_removesMonth() {
        TrainerSummary existing = trainerWithMonth(2026, 5, 10);
        when(trainerSummaryRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existing));

        TrainerWorkloadRequest request = buildRequest(10, ActionType.DELETE); // exactly 10

        service.process(request);

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).save(captor.capture());

        TrainingYear savedYear = captor.getValue().getYearList().get(0);
        assertThat(savedYear.getMonthList()).isEmpty();
        assertThat(captor.getValue().getYearList()).hasSize(1);
    }
}

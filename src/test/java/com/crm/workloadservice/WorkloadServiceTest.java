package com.crm.workloadservice;

import com.crm.workloadservice.dao.TrainerSummaryRepository;
import com.crm.workloadservice.dto.ActionType;
import com.crm.workloadservice.dto.TrainerSummaryResponse;
import com.crm.workloadservice.dto.TrainerWorkloadRequest;
import com.crm.workloadservice.exception.TrainerNotFoundException;
import com.crm.workloadservice.model.TrainerSummary;
import com.crm.workloadservice.model.TrainingMonth;
import com.crm.workloadservice.model.TrainingYear;
import com.crm.workloadservice.service.WorkloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {

    @Mock
    private TrainerSummaryRepository trainerSummaryRepository;

    private WorkloadService workloadService;

    @BeforeEach
    void setUp() {
        workloadService = new WorkloadService(trainerSummaryRepository);
    }

    private TrainerWorkloadRequest buildRequest(ActionType actionType, int duration, LocalDate date) {
        return new TrainerWorkloadRequest(
                "john.doe", "John", "Doe", true, date, duration, actionType);
    }

    @Test
    void process_add_newTrainer_createsDocumentWithInitialDuration() {
        when(trainerSummaryRepository.searchByUsername("john.doe")).thenReturn(Optional.empty());

        workloadService.process(buildRequest(ActionType.ADD, 60, LocalDate.of(2026, 3, 10)));

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).save(captor.capture());
        TrainerSummary saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("john.doe");
        assertThat(saved.getStatus()).isTrue();
        assertThat(saved.getYears()).hasSize(1);
        assertThat(saved.getYears().get(0).getYear()).isEqualTo(2026);
        assertThat(saved.getYears().get(0).getMonths().get(0).getMonth()).isEqualTo(3);
        assertThat(saved.getYears().get(0).getMonths().get(0).getTrainingsSummaryDuration()).isEqualTo(60);
    }

    @Test
    void process_add_existingTrainerExistingMonth_addsDurationToExistingValue() {
        TrainerSummary existing = new TrainerSummary();
        existing.setUsername("john.doe");
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setStatus(true);
        TrainingMonth month = new TrainingMonth(3, 60);
        TrainingYear year = new TrainingYear(2026, new ArrayList<>(List.of(month)));
        existing.setYears(new ArrayList<>(List.of(year)));

        when(trainerSummaryRepository.searchByUsername("john.doe")).thenReturn(Optional.of(existing));

        workloadService.process(buildRequest(ActionType.ADD, 40, LocalDate.of(2026, 3, 15)));

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).updateByUsername(eq("john.doe"), captor.capture());

        TrainingMonth updatedMonth = captor.getValue().getYears().get(0).getMonths().get(0);
        assertThat(updatedMonth.getTrainingsSummaryDuration()).isEqualTo(100);
    }

    @Test
    void process_add_existingTrainerNewMonth_createsMonthWithGivenDuration() {
        TrainerSummary existing = new TrainerSummary();
        existing.setUsername("john.doe");
        existing.setYears(new ArrayList<>(List.of(new TrainingYear(2026, new ArrayList<>()))));

        when(trainerSummaryRepository.searchByUsername("john.doe")).thenReturn(Optional.of(existing));

        workloadService.process(buildRequest(ActionType.ADD, 45, LocalDate.of(2026, 5, 1)));

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).updateByUsername(eq("john.doe"), captor.capture());

        TrainingYear year2026 = captor.getValue().getYears().get(0);
        assertThat(year2026.getMonths()).extracting(TrainingMonth::getMonth).contains(5);
        assertThat(year2026.getMonths()).filteredOn(m -> m.getMonth() == 5)
                .extracting(TrainingMonth::getTrainingsSummaryDuration).containsExactly(45);
    }

    @Test
    void process_delete_reducesDuration() {
        TrainerSummary existing = new TrainerSummary();
        existing.setUsername("john.doe");
        TrainingMonth month = new TrainingMonth(3, 100);
        existing.setYears(new ArrayList<>(List.of(new TrainingYear(2026, new ArrayList<>(List.of(month))))));

        when(trainerSummaryRepository.searchByUsername("john.doe")).thenReturn(Optional.of(existing));

        workloadService.process(buildRequest(ActionType.DELETE, 40, LocalDate.of(2026, 3, 20)));

        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(trainerSummaryRepository).updateByUsername(eq("john.doe"), captor.capture());
        assertThat(captor.getValue().getYears().get(0).getMonths().get(0).getTrainingsSummaryDuration()).isEqualTo(60);
    }

    @Test
    void process_delete_whenExceedsRecordedDuration_throwsIllegalArgumentException() {
        TrainerSummary existing = new TrainerSummary();
        existing.setUsername("john.doe");
        TrainingMonth month = new TrainingMonth(3, 20);
        existing.setYears(new ArrayList<>(List.of(new TrainingYear(2026, new ArrayList<>(List.of(month))))));

        when(trainerSummaryRepository.searchByUsername("john.doe")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> workloadService.process(buildRequest(ActionType.DELETE, 40, LocalDate.of(2026, 3, 20))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void process_whenTrainingDurationNotPositive_throwsIllegalArgumentExceptionAndSkipsRepository() {
        assertThatThrownBy(() -> workloadService.process(buildRequest(ActionType.ADD, 0, LocalDate.of(2026, 3, 20))))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(trainerSummaryRepository);
    }

    @Test
    void getSummary_whenTrainerMissing_throwsTrainerNotFoundException() {
        when(trainerSummaryRepository.searchByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workloadService.getSummary("missing"))
                .isInstanceOf(TrainerNotFoundException.class);
    }

    @Test
    void getSummary_whenTrainerExists_returnsMappedResponse() {
        TrainerSummary existing = new TrainerSummary();
        existing.setUsername("john.doe");
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setStatus(true);
        TrainingMonth month = new TrainingMonth(3, 60);
        existing.setYears(new ArrayList<>(List.of(new TrainingYear(2026, new ArrayList<>(List.of(month))))));

        when(trainerSummaryRepository.searchByUsername("john.doe")).thenReturn(Optional.of(existing));

        TrainerSummaryResponse response = workloadService.getSummary("john.doe");

        assertThat(response.getUsername()).isEqualTo("john.doe");
        assertThat(response.getYears()).hasSize(1);
        assertThat(response.getYears().get(0).getMonths().get(0).getTrainingSummaryDuration()).isEqualTo(60);
    }
}

package com.crm.workloadservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainingMonth {

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    @Min(0)
    private Integer trainingsSummaryDuration;
}

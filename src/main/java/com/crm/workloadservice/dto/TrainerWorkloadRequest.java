package com.crm.workloadservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainerWorkloadRequest {

    @NotBlank
    @NotNull
    private String username;

    @NotBlank
    @NotNull
    private String firstName;

    @NotBlank
    @NotNull
    private String lastName;

    @NotNull
    private Boolean isActive;

    @NotNull
    private LocalDate trainingDate;

    @NotNull
    private Integer trainingDuration;

    @NotNull
    private ActionType actionType;

}

package com.crm.workloadservice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainingYear {

    @NotNull
    private Integer year;

    @Valid
    private List<TrainingMonth> months = new ArrayList<>();

    public void addMonth(TrainingMonth month) {
        this.months.add(month);
    }
}

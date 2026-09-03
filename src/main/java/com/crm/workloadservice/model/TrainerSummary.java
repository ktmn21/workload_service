package com.crm.workloadservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String firstName;
    private String lastName;
    private Boolean status;

    @OneToMany(mappedBy = "trainerSummary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingYear> yearList = new ArrayList<>();

    public void addYear(TrainingYear year){
        yearList.add(year);
        year.setTrainerSummary(this);
    }

}

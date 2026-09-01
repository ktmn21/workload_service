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
public class TrainingYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer year;

    @ManyToOne
    @JoinColumn(name = "trainer_id")
    private TrainerSummary trainerSummary;

    @OneToMany(mappedBy = "trainingYear", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingMonth> monthList = new ArrayList<>();

    public void addMonth(TrainingMonth month){
        monthList.add(month);
        month.setTrainingYear(this);
    }
}

package com.crm.workloadservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TrainingMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month_number")
    private Integer month;
    private Integer duration;

    @ManyToOne
    @JoinColumn(name = "year_id")
    private TrainingYear trainingYear;

}

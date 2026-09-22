package com.crm.workloadservice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "trainer_summary")
@CompoundIndexes({
        @CompoundIndex(name = "first_last_name_idx", def = "{'firstName': 1, 'lastName': 1}")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSummary {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true, name = "username_idx")
    @Field("username")
    private String username;

    @NotBlank
    @Field("firstName")
    private String firstName;

    @NotBlank
    @Field("lastName")
    private String lastName;

    @NotNull
    @Field("status")
    private Boolean status;

    @Valid
    @Field("years")
    private List<TrainingYear> years = new ArrayList<>();

    @Version
    private Long version;

    public void addYear(TrainingYear year) {
        this.years.add(year);
    }
}

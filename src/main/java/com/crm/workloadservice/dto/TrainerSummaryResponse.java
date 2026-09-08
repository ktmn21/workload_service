package com.crm.workloadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class TrainerSummaryResponse {
    private String username;
    private String firstName;
    private String lastName;
    private Boolean status;
    private List<YearSummary> years;

    @Getter
    @AllArgsConstructor
    public static class YearSummary {
        private Integer year;
        private List<MonthSummary> months;
    }

    @Getter
    @AllArgsConstructor
    public static class MonthSummary {
        private Integer month;
        private Integer trainingSummaryDuration;
    }
}
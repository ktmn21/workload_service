package com.crm.workloadservice.controller;

import com.crm.workloadservice.dto.TrainerSummaryResponse;
import com.crm.workloadservice.dto.TrainerWorkloadRequest;
import com.crm.workloadservice.service.WorkloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workload")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;

    @PostMapping
    public ResponseEntity<Void> processWorkload(@Valid @RequestBody TrainerWorkloadRequest request) {
        workloadService.process(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    public ResponseEntity<TrainerSummaryResponse> getSummary(@PathVariable String username) {
        return ResponseEntity.ok(workloadService.getSummary(username));
    }
}
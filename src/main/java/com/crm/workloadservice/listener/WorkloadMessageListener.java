package com.crm.workloadservice.listener;

import com.crm.workloadservice.dto.TrainerWorkloadRequest;
import com.crm.workloadservice.service.WorkloadService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class WorkloadMessageListener {

    private static final Logger log = LoggerFactory.getLogger(WorkloadMessageListener.class);

    private final WorkloadService workloadService;
    private final Validator validator;
    private final JmsTemplate jmsTemplate;

    @Value("${workload.queue.dlq-name}")
    private String dlqName;

    public WorkloadMessageListener(WorkloadService workloadService, Validator validator, JmsTemplate jmsTemplate) {
        this.workloadService = workloadService;
        this.validator = validator;
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = "${workload.queue.name}")
    public void onMessage(TrainerWorkloadRequest request) {
        Set<ConstraintViolation<TrainerWorkloadRequest>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            log.warn("Invalid workload message routing to DLQ:{}", violations);
            jmsTemplate.convertAndSend(dlqName, request);
            return;
        }

        try {
            workloadService.process(request);
            log.info("Processed workload message; trainer={}, action={}",
                    request.getUsername(), request.getActionType());
        } catch (Exception e) {
            log.error("Error processing workload message will be redelivered: {}", e.getMessage(), e);
            throw e;
        }
    }
}
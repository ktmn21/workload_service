package com.crm.workloadservice.listener;

import com.crm.workloadservice.dto.TrainerWorkloadRequest;
import com.crm.workloadservice.logging.TransactionIdFilter;
import com.crm.workloadservice.service.WorkloadService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@Profile("!test")
public class WorkloadMessageListener {

    private static final Logger txLog = LoggerFactory.getLogger("TRANSACTION");
    private static final Logger opLog = LoggerFactory.getLogger(WorkloadMessageListener.class);

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
    public void onMessage(TrainerWorkloadRequest request,
                          @Header(name = TransactionIdFilter.TX_HEADER, required = false) String incomingTxId) {
        String txId = (incomingTxId == null || incomingTxId.isBlank()) ? UUID.randomUUID().toString() : incomingTxId;
        MDC.put(TransactionIdFilter.TX_MDC_KEY, txId);
        try {
            txLog.info("[txId={}] RECEIVED message username={} actionType={}",
                    txId, request.getUsername(), request.getActionType());

            Set<ConstraintViolation<TrainerWorkloadRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                opLog.warn("[txId={}] operation=VALIDATE result=FAILED violations={}", txId, violations);
                jmsTemplate.convertAndSend(dlqName, request);
                txLog.warn("[txId={}] ROUTED_TO_DLQ username={}", txId, request.getUsername());
                return;
            }

            workloadService.process(request);
            txLog.info("[txId={}] PROCESSED message username={} actionType={}",
                    txId, request.getUsername(), request.getActionType());
        } catch (Exception e) {
            opLog.error("[txId={}] operation=PROCESS result=ERROR message={}", txId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}

package com.crm.workloadservice.exception;

public class TrainerNotFoundException extends RuntimeException {

    public TrainerNotFoundException(String message) {
        super(message);
    }
}
package com.backend2.backend2_notificationservice.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long customerId) {
        super("Customer with id " + customerId + " was not found");
    }
}

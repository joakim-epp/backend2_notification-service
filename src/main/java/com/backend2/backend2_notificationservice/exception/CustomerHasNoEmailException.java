package com.backend2.backend2_notificationservice.exception;

public class CustomerHasNoEmailException extends RuntimeException {

    public CustomerHasNoEmailException(Long customerId) {
        super("Customer with id " + customerId + " has no email address to send a confirmation to");
    }
}

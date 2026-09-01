package com.backend2.backend2_notificationservice.client;

import com.backend2.backend2_notificationservice.exception.CustomerNotFoundException;
import com.backend2.backend2_notificationservice.exception.CustomerServiceUnavailableException;
import feign.FeignException;
import org.springframework.stereotype.Component;

/**
 * Turns the customer service's answers into this service's own exceptions, so nothing outside this
 * class has to know that Feign is what makes the call.
 */
@Component
public class CustomerClient {

    private final CustomerApi customerApi;

    public CustomerClient(CustomerApi customerApi) {
        this.customerApi = customerApi;
    }

    /**
     * A 404 means the customer really is gone and the caller sent a bad id, so it stays a 404.
     * Everything else, timeout, connection refused, 5xx, unreadable body, is our problem and
     * becomes a 503: the confirmation is not sent, and the caller can try again.
     */
    public CustomerSummary findById(Long customerId) {
        try {
            CustomerSummary customer = customerApi.findById(customerId);
            if (customer == null) {
                throw new CustomerServiceUnavailableException(
                        "Customer service responded without a body", null);
            }
            return customer;
        } catch (FeignException.NotFound e) {
            throw new CustomerNotFoundException(customerId);
        } catch (FeignException e) {
            throw new CustomerServiceUnavailableException("Could not reach the customer service", e);
        }
    }
}

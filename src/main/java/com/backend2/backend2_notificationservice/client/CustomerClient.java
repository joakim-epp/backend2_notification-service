package com.backend2.backend2_notificationservice.client;

import com.backend2.backend2_notificationservice.exception.CustomerNotFoundException;
import com.backend2.backend2_notificationservice.exception.CustomerServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CustomerClient {

    private final RestClient restClient;
    private final String baseUrl;

    public CustomerClient(RestClient restClient, @Value("${customer.service.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    /**
     * A 404 means the customer really is gone and the caller sent a bad id, so it stays a 404.
     * Everything else, timeout, connection refused, 5xx, unreadable body, is our problem and
     * becomes a 503: the confirmation is not sent, and the caller can try again.
     */
    public CustomerSummary findById(Long customerId) {
        try {
            CustomerSummary customer = restClient.get()
                    .uri(baseUrl + "/api/customers/{id}", customerId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new CustomerNotFoundException(customerId);
                    })
                    .body(CustomerSummary.class);

            if (customer == null) {
                throw new CustomerServiceUnavailableException(
                        "Customer service responded without a body", null);
            }
            return customer;
        } catch (RestClientException e) {
            throw new CustomerServiceUnavailableException("Could not reach the customer service", e);
        }
    }
}

package com.backend2.backend2_notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "${customer.service.url}")
public interface CustomerApi {

    @GetMapping("/api/customers/{id}")
    CustomerSummary findById(@PathVariable("id") Long id);
}

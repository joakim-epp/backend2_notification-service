package com.backend2.backend2_notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.backend2.backend2_notificationservice.dto.LoginRequest;
import com.backend2.backend2_notificationservice.dto.LoginResponse;

@FeignClient(name = "customer-service", url = "${customer.service.url}")
public interface CustomerApi {

    @PostMapping("/api/auth/login")
    LoginResponse login(@RequestBody LoginRequest request);

    @GetMapping("/api/customers/{id}")
    CustomerSummary findById(@PathVariable("id") Long id);
}

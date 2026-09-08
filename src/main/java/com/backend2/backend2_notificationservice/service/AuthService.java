package com.backend2.backend2_notificationservice.service;

import com.backend2.backend2_notificationservice.client.CustomerApi;
import com.backend2.backend2_notificationservice.dto.LoginRequest;
import com.backend2.backend2_notificationservice.dto.LoginResponse;
import com.backend2.backend2_notificationservice.exception.CustomerServiceUnavailableException;
import feign.FeignException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final CustomerApi customerApi;

    public AuthService(CustomerApi customerApi) {
        this.customerApi = customerApi;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            LoginResponse response = customerApi.login(request);
            if (response == null || response.token() == null || response.token().isBlank()) {
                throw new CustomerServiceUnavailableException("Customer service returned no token", null);
            }
            return response;
        } catch (FeignException.Unauthorized e) {
            throw new BadCredentialsException("Fel användarnamn eller lösenord");
        } catch (FeignException e) {
            throw new CustomerServiceUnavailableException("Could not reach the customer service", e);
        }
    }
}

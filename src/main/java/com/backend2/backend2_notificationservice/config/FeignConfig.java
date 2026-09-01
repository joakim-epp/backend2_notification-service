package com.backend2.backend2_notificationservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

     //Passes the caller's token on to the next service.

    @Bean
    RequestInterceptor authorizationForwarder() {
        return template -> {
            String token = incomingAuthHeader();
            if (token != null) {
                template.header(HttpHeaders.AUTHORIZATION, token);
            }
        };
    }

    private String incomingAuthHeader() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}

package com.backend2.backend2_notificationservice.dto;

public record LoginResponse(String token) {
    @Override
    public String toString() {
        return "LoginResponse[token redacted]";
    }
}

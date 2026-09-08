package com.backend2.backend2_notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    @Override
    public String toString() {
        return "LoginRequest[credentials redacted]";
    }
}

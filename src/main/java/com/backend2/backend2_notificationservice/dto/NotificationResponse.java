package com.backend2.backend2_notificationservice.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long customerId,
        Long bookingId,
        String recipient,
        String message,
        Instant createdAt
) {}

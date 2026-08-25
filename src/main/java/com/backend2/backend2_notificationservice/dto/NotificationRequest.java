package com.backend2.backend2_notificationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record NotificationRequest(
        @NotNull(message = "Customer id is required")
        @Positive(message = "Customer id must be a positive number")
        Long customerId,

        @NotNull(message = "Booking id is required")
        @Positive(message = "Booking id must be a positive number")
        Long bookingId,

        @NotNull(message = "Check-in date is required")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOut
) {}

package com.backend2.backend2_notificationservice.controller;

import com.backend2.backend2_notificationservice.dto.NotificationRequest;
import com.backend2.backend2_notificationservice.dto.NotificationResponse;
import com.backend2.backend2_notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Called by the booking service once a booking is stored. No Location header: there is no
     * endpoint for a single notification, the log is read per customer.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
        return notificationService.send(request);
    }

    @GetMapping
    public List<NotificationResponse> findByCustomer(@RequestParam @Positive Long customerId) {
        return notificationService.findByCustomer(customerId);
    }
}

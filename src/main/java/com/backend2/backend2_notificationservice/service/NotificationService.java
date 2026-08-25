package com.backend2.backend2_notificationservice.service;

import com.backend2.backend2_notificationservice.client.CustomerClient;
import com.backend2.backend2_notificationservice.client.CustomerSummary;
import com.backend2.backend2_notificationservice.dto.NotificationRequest;
import com.backend2.backend2_notificationservice.dto.NotificationResponse;
import com.backend2.backend2_notificationservice.exception.CustomerHasNoEmailException;
import com.backend2.backend2_notificationservice.model.Notification;
import com.backend2.backend2_notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final CustomerClient customerClient;

    public NotificationService(NotificationRepository notificationRepository, CustomerClient customerClient) {
        this.notificationRepository = notificationRepository;
        this.customerClient = customerClient;
    }

    /**
     * Deliberately not @Transactional: the customer lookup below may take up to two seconds, and a
     * transaction spanning it would hold a pooled connection open the whole time. The save is the
     * only write, and SimpleJpaRepository.save runs in its own short transaction.
     */
    public NotificationResponse send(NotificationRequest request) {
        CustomerSummary customer = customerClient.findById(request.customerId());
        if (customer.email() == null || customer.email().isBlank()) {
            throw new CustomerHasNoEmailException(request.customerId());
        }

        Notification notification = new Notification();
        notification.setCustomerId(request.customerId());
        notification.setBookingId(request.bookingId());
        notification.setRecipient(customer.email());
        notification.setMessage(compose(customer, request));

        Notification saved = notificationRepository.save(notification);

        // Sending is logging. There is no SMTP server, and the row above is the delivery record.
        log.info("Booking confirmation for booking {} sent to {}", saved.getBookingId(), saved.getRecipient());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findByCustomer(Long customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private String compose(CustomerSummary customer, NotificationRequest request) {
        return "Hi %s, your booking #%d is confirmed: %s to %s. Welcome to Pensionatet."
                .formatted(customer.firstName(), request.bookingId(), request.checkIn(), request.checkOut());
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getCustomerId(), n.getBookingId(),
                n.getRecipient(), n.getMessage(), n.getCreatedAt());
    }
}

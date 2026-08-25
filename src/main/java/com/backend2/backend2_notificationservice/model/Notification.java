package com.backend2.backend2_notificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One sent booking confirmation. Rows are never updated or deleted, the table is the log.
 *
 * <p>Recipient and message are stored as they looked when the confirmation went out. Reading them
 * back from the customer service later would rewrite history the moment a customer changes their
 * email address.
 */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_customer_id", columnList = "customerId"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owned by the customer service. No foreign key, the customer lives in another database. */
    @Column(nullable = false)
    private Long customerId;

    /** Owned by the booking service, same reasoning as customerId. */
    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void stampCreatedAt() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

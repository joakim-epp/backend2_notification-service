package com.backend2.backend2_notificationservice.repository;

import com.backend2.backend2_notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Newest first, which is the order the log is read in. */
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}

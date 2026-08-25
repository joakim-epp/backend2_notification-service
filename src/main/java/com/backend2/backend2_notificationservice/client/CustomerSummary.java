package com.backend2.backend2_notificationservice.client;

/** The fields this service needs out of the customer service's response. The rest is ignored. */
public record CustomerSummary(Long id, String firstName, String lastName, String email) {}

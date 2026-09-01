package com.backend2.backend2_notificationservice;

import org.springframework.boot.SpringApplication;

public class TestBackend2NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(Backend2NotificationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

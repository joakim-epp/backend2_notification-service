package com.backend2.backend2_notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class Backend2NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Backend2NotificationServiceApplication.class, args);
    }

}

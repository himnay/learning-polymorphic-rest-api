package com.org.learning.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class PaymentApiApplication {

    public static void main(String[] args) {
        var application = new SpringApplication(PaymentApiApplication.class);
        // config is per-profile only (application-<profile>.yml) — fall back to local
        application.setDefaultProperties(Map.of("spring.profiles.default", "local"));
        application.run(args);
    }
}

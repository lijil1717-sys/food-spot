package com.foodspot.foodspot_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.foodspot.foodspot_user")
public class FoodspotUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodspotUserApplication.class, args);
    }
}
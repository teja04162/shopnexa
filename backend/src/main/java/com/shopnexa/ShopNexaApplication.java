package com.shopnexa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShopNexaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopNexaApplication.class, args);
    }
}

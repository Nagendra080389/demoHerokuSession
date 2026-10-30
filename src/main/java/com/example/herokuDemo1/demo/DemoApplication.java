package com.example.herokuDemo1.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for FNOL Intake Service.
 *
 * Enables JPA repositories for claim persistence.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.herokuDemo1.fnol.repository")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

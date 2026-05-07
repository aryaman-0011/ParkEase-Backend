package com.parkease.parkinglot_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot entry point for the Parking Lot microservice.
 * Manages CRUD operations for parking lot entities.
 */
@SpringBootApplication
@EnableAsync
public class ParkinglotServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkinglotServiceApplication.class, args);
	}

}

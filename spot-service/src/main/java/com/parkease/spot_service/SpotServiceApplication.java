package com.parkease.spot_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Spot microservice.
 * Manages individual parking spots within lots.
 */
@SpringBootApplication
public class SpotServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotServiceApplication.class, args);
	}

}

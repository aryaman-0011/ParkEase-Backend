package com.parkease.spot_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Spring Boot entry point for the Spot microservice.
 * Manages individual parking spots within lots.
 */
@SpringBootApplication
@EnableFeignClients
public class SpotServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotServiceApplication.class, args);
	}

}

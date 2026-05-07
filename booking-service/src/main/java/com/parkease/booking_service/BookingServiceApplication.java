package com.parkease.booking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot entry point for the Booking microservice.
 * Manages parking spot reservations, check-in/out, and time-slot conflict detection.
 */
@SpringBootApplication
@EnableAsync
public class BookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}

}

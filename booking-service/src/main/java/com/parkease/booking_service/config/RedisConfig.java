package com.parkease.booking_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

/**
 * Redis configuration for booking-service.
 * Provides a distributed lock registry to prevent double-booking race conditions.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisLockRegistry provides distributed locks across booking-service instances.
     * Lock keys are prefixed with "booking-lock:" and auto-expire after 10 seconds.
     */
    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory connectionFactory) {
        return new RedisLockRegistry(connectionFactory, "booking-lock", 10_000L);
    }
}

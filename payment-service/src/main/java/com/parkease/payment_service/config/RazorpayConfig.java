package com.parkease.payment_service.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Razorpay SDK configuration.
 * Initializes the RazorpayClient bean with API key and secret from properties.
 */
@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }

    @Bean
    public String razorpayKeyId() {
        return keyId;
    }

    @Bean
    public String razorpayKeySecret() {
        return keySecret;
    }
}

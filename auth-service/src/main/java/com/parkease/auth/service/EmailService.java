package com.parkease.auth.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp);

    void sendLoginNotificationEmail(String toEmail, String fullName, String loginMethod);
}

package com.parkease.auth.service.impl;

import com.parkease.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("ParkEase — Your Password Reset Code");
            helper.setText(buildOtpEmailHtml(otp), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again later.");
        }
    }

    @Override
    @Async
    public void sendLoginNotificationEmail(String toEmail, String fullName, String loginMethod) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("ParkEase — New Sign-In to Your Account");
            helper.setText(buildLoginNotificationHtml(fullName, loginMethod), true);

            mailSender.send(message);
            log.info("Login notification email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send login notification email to {}: {}", toEmail, e.getMessage());
            // Don't throw — login notification is non-critical
        }
    }

    private String buildOtpEmailHtml(String otp) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="margin: 0; padding: 0; background-color: #0a0a0a; font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Arial, sans-serif;">
                  <div style="max-width: 520px; margin: 0 auto; padding: 32px 16px;">
                
                    <!-- Card -->
                    <div style="background-color: #171717; border-radius: 16px; overflow: hidden; border: 1px solid rgba(255,255,255,0.06);">
                
                      <!-- Header -->
                      <div style="padding: 36px 32px 28px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.06);">
                        <div style="display: inline-block; background-color: #ffffff; width: 44px; height: 44px; border-radius: 12px; line-height: 44px; text-align: center; margin-bottom: 16px;">
                          <span style="font-size: 22px; font-weight: 900; color: #0a0a0a;">P</span>
                        </div>
                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.3px;">ParkEase</h1>
                        <p style="color: #737373; margin: 8px 0 0; font-size: 14px; font-weight: 400;">Password Reset Request</p>
                      </div>
                
                      <!-- Body -->
                      <div style="padding: 32px;">
                        <p style="color: #d4d4d4; font-size: 15px; line-height: 1.7; margin: 0 0 28px;">
                          We received a request to reset your password. Use the verification code below to proceed:
                        </p>
                
                        <!-- OTP Box -->
                        <div style="background-color: #0a0a0a; border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 24px; text-align: center; margin: 0 0 28px;">
                          <span style="font-size: 38px; font-weight: 700; letter-spacing: 10px; color: #ffffff; font-family: 'Courier New', monospace;">%s</span>
                        </div>
                
                        <!-- Info pills -->
                        <div style="margin: 0 0 24px;">
                          <div style="display: inline-block; background-color: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; padding: 10px 16px; margin-bottom: 8px;">
                            <span style="color: #a3a3a3; font-size: 13px;">⏱️ Expires in <strong style="color: #ffffff;">5 minutes</strong></span>
                          </div>
                        </div>
                
                        <p style="color: #525252; font-size: 13px; line-height: 1.6; margin: 0;">
                          If you didn't request this code, you can safely ignore this email. Your account remains secure.
                        </p>
                      </div>
                
                      <!-- Footer -->
                      <div style="padding: 20px 32px; border-top: 1px solid rgba(255,255,255,0.06); text-align: center;">
                        <p style="color: #404040; font-size: 12px; margin: 0;">© 2026 ParkEase. All rights reserved.</p>
                      </div>
                
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(otp);
    }

    private String buildLoginNotificationHtml(String fullName, String loginMethod) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        String displayName = (fullName != null && !fullName.isBlank()) ? fullName : "there";
        String method = (loginMethod != null && !loginMethod.isBlank()) ? loginMethod : "Email & Password";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="margin: 0; padding: 0; background-color: #0a0a0a; font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Arial, sans-serif;">
                  <div style="max-width: 520px; margin: 0 auto; padding: 32px 16px;">
                
                    <!-- Card -->
                    <div style="background-color: #171717; border-radius: 16px; overflow: hidden; border: 1px solid rgba(255,255,255,0.06);">
                
                      <!-- Header -->
                      <div style="padding: 36px 32px 28px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.06);">
                        <div style="display: inline-block; background-color: #ffffff; width: 44px; height: 44px; border-radius: 12px; line-height: 44px; text-align: center; margin-bottom: 16px;">
                          <span style="font-size: 22px; font-weight: 900; color: #0a0a0a;">P</span>
                        </div>
                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.3px;">ParkEase</h1>
                        <p style="color: #737373; margin: 8px 0 0; font-size: 14px; font-weight: 400;">Security Alert</p>
                      </div>
                
                      <!-- Body -->
                      <div style="padding: 32px;">
                        <p style="color: #d4d4d4; font-size: 15px; line-height: 1.7; margin: 0 0 24px;">
                          Hi <strong style="color: #ffffff;">%s</strong>, we detected a new sign-in to your ParkEase account.
                        </p>
                
                        <!-- Login Details Card -->
                        <div style="background-color: #0a0a0a; border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 20px 24px; margin: 0 0 24px;">
                
                          <!-- Sign-in method -->
                          <div style="display: flex; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid rgba(255,255,255,0.06);">
                            <div style="width: 100%%;">
                              <div style="color: #525252; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;">Sign-in Method</div>
                              <div style="color: #ffffff; font-size: 15px; font-weight: 600;">%s</div>
                            </div>
                          </div>
                
                          <!-- Time -->
                          <div style="width: 100%%;">
                            <div style="color: #525252; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;">Date & Time</div>
                            <div style="color: #ffffff; font-size: 15px; font-weight: 600;">%s</div>
                          </div>
                
                        </div>
                
                        <!-- Success indicator -->
                        <div style="display: inline-block; background-color: rgba(34,197,94,0.08); border: 1px solid rgba(34,197,94,0.15); border-radius: 8px; padding: 10px 16px; margin-bottom: 24px;">
                          <span style="color: #4ade80; font-size: 13px;">✓ Login successful</span>
                        </div>
                
                        <p style="color: #525252; font-size: 13px; line-height: 1.6; margin: 0;">
                          If this wasn't you, please secure your account immediately by changing your password.
                        </p>
                      </div>
                
                      <!-- Footer -->
                      <div style="padding: 20px 32px; border-top: 1px solid rgba(255,255,255,0.06); text-align: center;">
                        <p style="color: #404040; font-size: 12px; margin: 0;">© 2026 ParkEase. All rights reserved.</p>
                      </div>
                
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(displayName, method, now);
    }
}

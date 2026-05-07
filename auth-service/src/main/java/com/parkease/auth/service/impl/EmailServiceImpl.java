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

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Welcome to ParkEase \uD83C\uDF89");
            helper.setText(buildWelcomeEmailHtml(fullName), true);

            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
            // Don't throw — welcome email is non-critical
        }
    }

    @Override
    public void sendReceiptEmail(String toEmail, String fullName, java.util.Map<String, String> d) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("ParkEase \u2014 Payment Receipt " + d.getOrDefault("receiptNumber", ""));
            helper.setText(buildReceiptEmailHtml(fullName, d), true);

            mailSender.send(message);
            log.info("Receipt email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send receipt email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send receipt email");
        }
    }

    private String buildWelcomeEmailHtml(String fullName) {
        String name = (fullName != null && !fullName.isBlank()) ? fullName : "there";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="margin: 0; padding: 0; background-color: #0a0a0a; font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Arial, sans-serif;">
                  <div style="max-width: 520px; margin: 0 auto; padding: 32px 16px;">
                    <div style="background-color: #171717; border-radius: 16px; overflow: hidden; border: 1px solid rgba(255,255,255,0.06);">
                      <!-- Header -->
                      <div style="padding: 36px 32px 28px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.06);">
                        <div style="display: inline-block; background-color: #ffffff; width: 44px; height: 44px; border-radius: 12px; line-height: 44px; text-align: center; margin-bottom: 16px;">
                          <span style="font-size: 22px; font-weight: 900; color: #0a0a0a;">P</span>
                        </div>
                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 700;">Welcome to ParkEase!</h1>
                        <p style="color: #737373; margin: 8px 0 0; font-size: 14px;">Your account has been created successfully</p>
                      </div>
                      <!-- Body -->
                      <div style="padding: 32px;">
                        <p style="color: #d4d4d4; font-size: 15px; line-height: 1.7; margin: 0 0 20px;">
                          Hi <strong style="color: #ffffff;">%s</strong>,
                        </p>
                        <p style="color: #d4d4d4; font-size: 15px; line-height: 1.7; margin: 0 0 24px;">
                          Thank you for joining ParkEase! You can now find, reserve, and manage parking spots with ease.
                        </p>
                        <div style="background-color: #0a0a0a; border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 20px; margin: 0 0 24px;">
                          <p style="color: #a3a3a3; font-size: 13px; line-height: 1.6; margin: 0;">
                            \u2705 Search nearby parking lots<br>
                            \u2705 Reserve spots in advance<br>
                            \u2705 Track your bookings &amp; payments<br>
                            \u2705 Get real-time notifications
                          </p>
                        </div>
                        <p style="color: #737373; font-size: 13px; line-height: 1.6; margin: 0;">
                          If you didn't create this account, you can safely ignore this email.
                        </p>
                      </div>
                      <!-- Footer -->
                      <div style="padding: 20px 32px; border-top: 1px solid rgba(255,255,255,0.06); text-align: center;">
                        <p style="color: #525252; font-size: 11px; margin: 0;">\u00A9 2026 ParkEase. All rights reserved.</p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name);
    }

    private String buildReceiptEmailHtml(String fullName, java.util.Map<String, String> d) {
        String name = (fullName != null && !fullName.isBlank()) ? fullName : "Customer";
        String amount = d.getOrDefault("amount", "0.00");
        String status = d.getOrDefault("status", "SUCCESS");
        String receiptNo = d.getOrDefault("receiptNumber", "—");
        String method = d.getOrDefault("paymentMethod", "—");
        String txnId = d.getOrDefault("transactionId", "—");
        String date = d.getOrDefault("date", "—");
        String lotName = d.getOrDefault("lotName", "—");
        String spot = d.getOrDefault("spotNumber", "—");
        String vehicle = d.getOrDefault("vehiclePlate", "—");
        String duration = d.getOrDefault("duration", "—");
        String description = d.getOrDefault("description", "Parking fee");

        String statusColor = switch (status) {
            case "SUCCESS" -> "#22c55e";
            case "REFUNDED" -> "#38bdf8";
            case "FAILED" -> "#ef4444";
            default -> "#eab308";
        };
        String statusBg = switch (status) {
            case "SUCCESS" -> "rgba(34,197,94,0.1)";
            case "REFUNDED" -> "rgba(56,189,248,0.1)";
            case "FAILED" -> "rgba(239,68,68,0.1)";
            default -> "rgba(234,179,8,0.1)";
        };

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="margin: 0; padding: 0; background-color: #0a0a0a; font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Arial, sans-serif;">
                  <div style="max-width: 520px; margin: 0 auto; padding: 32px 16px;">

                    <!-- Card -->
                    <div style="background-color: #171717; border-radius: 16px; overflow: hidden; border: 1px solid rgba(255,255,255,0.06);">

                      <!-- Header -->
                      <div style="background-color: #3b2e1e; padding: 28px 32px;">
                        <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                          <td><span style="font-size: 22px; font-weight: 800; color: #f5ece0; letter-spacing: -0.3px;">ParkEase</span>
                            <br><span style="font-size: 11px; color: #c4b9ab; text-transform: uppercase; letter-spacing: 1px;">Payment Receipt</span></td>
                          <td align="right" valign="top">
                            <span style="font-size: 11px; color: #c4b9ab; font-family: monospace;">%s</span><br>
                            <span style="display: inline-block; margin-top: 6px; padding: 3px 10px; border-radius: 6px; font-size: 10px; font-weight: 700; color: %s; background-color: %s;">%s</span>
                          </td>
                        </tr></table>
                      </div>

                      <!-- Amount -->
                      <div style="padding: 28px 32px; border-bottom: 1px solid rgba(255,255,255,0.06);">
                        <p style="color: #737373; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 6px;">Amount Paid</p>
                        <p style="color: #ffffff; font-size: 32px; font-weight: 800; margin: 0; letter-spacing: -1px;">Rs. %s</p>
                      </div>

                      <!-- Greeting -->
                      <div style="padding: 24px 32px 4px;">
                        <p style="color: #d4d4d4; font-size: 14px; margin: 0;">Hi <strong style="color: #fff;">%s</strong>, here's your payment receipt.</p>
                      </div>

                      <!-- Payment Details -->
                      <div style="padding: 20px 32px;">
                        <p style="color: #8B5E3C; font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 12px;">Payment Details</p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="font-size: 13px;">
                          <tr><td style="color: #737373; padding: 6px 0;">Payment Method</td><td align="right" style="color: #e5e5e5; font-weight: 600; padding: 6px 0;">%s</td></tr>
                          <tr><td style="color: #737373; padding: 6px 0;">Transaction ID</td><td align="right" style="color: #e5e5e5; font-weight: 600; padding: 6px 0; font-family: monospace; font-size: 11px;">%s</td></tr>
                          <tr><td style="color: #737373; padding: 6px 0;">Date & Time</td><td align="right" style="color: #e5e5e5; font-weight: 600; padding: 6px 0;">%s</td></tr>
                        </table>
                      </div>

                      <!-- Divider -->
                      <div style="padding: 0 32px;"><div style="border-top: 1px solid rgba(255,255,255,0.06);"></div></div>

                      <!-- Booking Details -->
                      <div style="padding: 20px 32px;">
                        <p style="color: #8B5E3C; font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 12px;">Booking Details</p>
                        <table width="100%%" cellpadding="0" cellspacing="0" style="font-size: 13px;">
                          <tr><td style="color: #737373; padding: 6px 0;">Parking Lot</td><td align="right" style="color: #e5e5e5; font-weight: 600; padding: 6px 0;">%s</td></tr>
                          <tr><td style="color: #737373; padding: 6px 0;">Spot</td><td align="right" style="color: #e5e5e5; font-weight: 600; padding: 6px 0;">%s</td></tr>
                          <tr><td style="color: #737373; padding: 6px 0;">Vehicle</td><td align="right" style="color: #e5e5e5; font-weight: 600; padding: 6px 0; font-family: monospace;">%s</td></tr>
                          <tr><td style="color: #737373; padding: 6px 0;">Duration</td><td align="right" style="color: #8B5E3C; font-weight: 700; padding: 6px 0;">%s</td></tr>
                          <tr><td style="color: #737373; padding: 6px 0;">Description</td><td align="right" style="color: #e5e5e5; font-weight: 600; padding: 6px 0;">%s</td></tr>
                        </table>
                      </div>

                      <!-- Footer -->
                      <div style="padding: 20px 32px; border-top: 1px solid rgba(255,255,255,0.06); text-align: center;">
                        <p style="color: #525252; font-size: 11px; margin: 0 0 4px;">This is a computer-generated receipt.</p>
                        <p style="color: #404040; font-size: 11px; margin: 0;">ParkEase · Smart Parking, Simplified</p>
                      </div>

                    </div>
                  </div>
                </body>
                </html>
                """.formatted(receiptNo, statusColor, statusBg, status, amount, name,
                method, txnId, date,
                lotName, spot, vehicle, duration, description);
    }
}

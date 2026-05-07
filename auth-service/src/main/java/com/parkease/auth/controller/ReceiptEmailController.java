package com.parkease.auth.controller;

import com.parkease.auth.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ReceiptEmailController {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping("/receipt/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> sendReceiptEmail(
            Authentication authentication,
            @RequestBody Map<String, String> receiptDetails) {

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Sending receipt email to {} for receipt #{}", user.getEmail(), receiptDetails.getOrDefault("receiptNumber", "N/A"));
        emailService.sendReceiptEmail(user.getEmail(), user.getFullName(), receiptDetails);

        return ResponseEntity.ok(Map.of("message", "Receipt sent to " + user.getEmail()));
    }
}

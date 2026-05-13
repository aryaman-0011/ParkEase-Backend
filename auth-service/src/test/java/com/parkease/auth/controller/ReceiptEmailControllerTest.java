package com.parkease.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkease.auth.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.service.EmailService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ReceiptEmailControllerTest {

    @Mock private EmailService emailService;
    @Mock private UserRepository userRepository;

    @Test
    void sendReceiptEmailSendsToAuthenticatedUser() {
        ReceiptEmailController controller = new ReceiptEmailController(emailService, userRepository);
        User user = User.builder().email("user@test.com").fullName("User").build();
        Map<String, String> receipt = Map.of("receiptNumber", "PE-123", "amount", "250");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));

        var response = controller.sendReceiptEmail(auth(), receipt);

        assertThat(response.getBody()).containsEntry("message", "Receipt sent to user@test.com");
        verify(emailService).sendReceiptEmail("user@test.com", "User", receipt);
    }

    @Test
    void sendReceiptEmailThrowsWhenUserIsMissing() {
        ReceiptEmailController controller = new ReceiptEmailController(emailService, userRepository);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.sendReceiptEmail(auth(), Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    private TestingAuthenticationToken auth() {
        return new TestingAuthenticationToken("user@test.com", null);
    }
}

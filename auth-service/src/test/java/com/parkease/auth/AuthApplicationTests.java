package com.parkease.auth;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class AuthApplicationTests {

    @Test
    void projectCompiles() {
        assertThat(true).isTrue();
    }

    @Test
    void createsApplicationInstance() {
        assertThat(new AuthApplication()).isNotNull();
    }

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            AuthApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(AuthApplication.class, args));
        }
    }
}

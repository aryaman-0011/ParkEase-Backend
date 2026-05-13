package com.parkease.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.Role;
import com.parkease.auth.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InternalControllerTest {

    @Mock private UserRepository userRepository;

    @Test
    void getAdminIdsReturnsAllAdminIds() {
        InternalController controller = new InternalController(userRepository);
        User firstAdmin = User.builder().id(1L).role(Role.ADMIN).build();
        User secondAdmin = User.builder().id(2L).role(Role.ADMIN).build();
        when(userRepository.findByRole(Role.ADMIN, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(firstAdmin, secondAdmin)));

        var response = controller.getAdminIds();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("ids", List.of(1L, 2L));
    }

    @Test
    void getUserIdsByRoleReturnsIdsForRequestedRole() {
        InternalController controller = new InternalController(userRepository);
        User driver = User.builder().id(10L).role(Role.DRIVER).build();
        when(userRepository.findByRole(Role.DRIVER, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(driver)));

        var response = controller.getUserIdsByRole(Role.DRIVER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("ids", List.of(10L));
    }
}

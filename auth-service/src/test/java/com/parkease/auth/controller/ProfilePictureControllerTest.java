package com.parkease.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkease.auth.entity.User;
import com.parkease.auth.enums.AuthProvider;
import com.parkease.auth.enums.Role;
import com.parkease.auth.exception.BadRequestException;
import com.parkease.auth.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProfilePictureControllerTest {

    private static final Path UPLOAD_DIR = Path.of("uploads", "avatars");

    @Mock private UserRepository userRepository;

    @AfterEach
    void cleanUploads() throws IOException {
        if (Files.exists(UPLOAD_DIR)) {
            try (var paths = Files.walk(UPLOAD_DIR)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best-effort cleanup for files created by this test class.
                    }
                });
            }
        }
    }

    @Test
    void uploadProfilePictureStoresFileAndUpdatesUser() throws IOException {
        ProfilePictureController controller = new ProfilePictureController(userRepository);
        Files.createDirectories(UPLOAD_DIR);
        Files.writeString(UPLOAD_DIR.resolve("old.png"), "old");
        User user = user();
        user.setProfilePicUrl("/avatars/old.png");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "new image".getBytes());

        var response = controller.uploadProfilePicture(auth(), file);

        assertThat(response.getBody().getProfilePicUrl()).startsWith("/avatars/avatar_1_").endsWith(".png");
        assertThat(Files.exists(UPLOAD_DIR.resolve("old.png"))).isFalse();
        try (var files = Files.list(UPLOAD_DIR)) {
            assertThat(files.count()).isEqualTo(1);
        }
        verify(userRepository).save(user);
    }

    @Test
    void uploadProfilePictureRejectsEmptyFile() {
        ProfilePictureController controller = new ProfilePictureController(userRepository);
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> controller.uploadProfilePicture(auth(), file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void uploadProfilePictureRejectsInvalidContentType() {
        ProfilePictureController controller = new ProfilePictureController(userRepository);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "text".getBytes());

        assertThatThrownBy(() -> controller.uploadProfilePicture(auth(), file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only JPEG");
    }

    @Test
    void uploadProfilePictureRejectsOversizedFile() {
        ProfilePictureController controller = new ProfilePictureController(userRepository);
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        org.mockito.Mockito.when(file.isEmpty()).thenReturn(false);
        org.mockito.Mockito.when(file.getSize()).thenReturn(2L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> controller.uploadProfilePicture(auth(), file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File size exceeds");
    }

    @Test
    void uploadProfilePictureRejectsMissingContentType() {
        ProfilePictureController controller = new ProfilePictureController(userRepository);
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        org.mockito.Mockito.when(file.isEmpty()).thenReturn(false);
        org.mockito.Mockito.when(file.getSize()).thenReturn(10L);
        org.mockito.Mockito.when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> controller.uploadProfilePicture(auth(), file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only JPEG");
    }

    @Test
    void removeProfilePictureDeletesExistingAvatarAndClearsUrl() throws IOException {
        ProfilePictureController controller = new ProfilePictureController(userRepository);
        Files.createDirectories(UPLOAD_DIR);
        Files.writeString(UPLOAD_DIR.resolve("old.png"), "old");
        User user = user();
        user.setProfilePicUrl("/avatars/old.png");
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.removeProfilePicture(auth());

        assertThat(response.getBody().getProfilePicUrl()).isNull();
        assertThat(Files.exists(UPLOAD_DIR.resolve("old.png"))).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void removeProfilePictureRejectsUnknownUser() {
        ProfilePictureController controller = new ProfilePictureController(userRepository);
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.removeProfilePicture(auth()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User not found");
    }

    private TestingAuthenticationToken auth() {
        return new TestingAuthenticationToken("user@test.com", null);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .fullName("User")
                .email("user@test.com")
                .role(Role.DRIVER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .build();
    }
}

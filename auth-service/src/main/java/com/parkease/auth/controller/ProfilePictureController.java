package com.parkease.auth.controller;

import com.parkease.auth.dto.UserResponse;
import com.parkease.auth.entity.User;
import com.parkease.auth.exception.BadRequestException;
import com.parkease.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

// Handles profile picture upload for authenticated users
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ProfilePictureController {

    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/avatars";
    private static final long MAX_SIZE = 2L * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    // Upload or replace the user's profile picture
    @PostMapping(value = "/profile/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("File size exceeds 2 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG, WebP, and GIF images are allowed");
        }

        // Resolve user
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Create upload directory
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Delete old avatar if it exists
        String oldPicUrl = user.getProfilePicUrl();
        if (oldPicUrl != null && oldPicUrl.contains("/avatars/")) {
            String oldFileName = oldPicUrl.substring(oldPicUrl.lastIndexOf("/") + 1);
            Path oldFile = uploadPath.resolve(oldFileName);
            Files.deleteIfExists(oldFile);
        }

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "avatar_" + user.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        // Save file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Update user profile pic URL (relative path served by static resources)
        String profilePicUrl = "/avatars/" + fileName;
        user.setProfilePicUrl(profilePicUrl);
        User saved = userRepository.save(user);
        log.info("Profile picture uploaded: user={} file={}", authentication.getName(), fileName);
        return ResponseEntity.ok(UserResponse.from(saved));
    }

    // Remove the user's profile picture
    @DeleteMapping("/profile/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> removeProfilePicture(Authentication authentication) throws IOException {
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String oldPicUrl = user.getProfilePicUrl();
        if (oldPicUrl != null && oldPicUrl.contains("/avatars/")) {
            String oldFileName = oldPicUrl.substring(oldPicUrl.lastIndexOf("/") + 1);
            Path oldFile = Paths.get(UPLOAD_DIR).resolve(oldFileName);
            Files.deleteIfExists(oldFile);
        }

        user.setProfilePicUrl(null);
        User saved = userRepository.save(user);
        log.info("Profile picture removed: user={}", authentication.getName());
        return ResponseEntity.ok(UserResponse.from(saved));
    }
}

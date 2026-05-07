package com.parkease.auth.security;

import com.parkease.auth.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class OAuth2UserInfoTest {

    @Nested
    @DisplayName("OAuth2UserInfoFactory")
    class Factory {
        @Test
        @DisplayName("should create GoogleOAuth2UserInfo for 'google'")
        void google() {
            OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("google", Map.of());
            assertThat(info).isInstanceOf(GoogleOAuth2UserInfo.class);
        }

        @Test
        @DisplayName("should be case-insensitive")
        void caseInsensitive() {
            assertThat(OAuth2UserInfoFactory.getOAuth2UserInfo("GOOGLE", Map.of()))
                    .isInstanceOf(GoogleOAuth2UserInfo.class);
        }

        @Test
        @DisplayName("should throw for unsupported provider")
        void unsupported() {
            assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo("facebook", Map.of()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unsupported");
        }

        @Test
        @DisplayName("should throw for github (removed provider)")
        void githubRemoved() {
            assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo("github", Map.of()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unsupported");
        }
    }

    @Nested
    @DisplayName("GoogleOAuth2UserInfo")
    class Google {
        @Test
        @DisplayName("should extract Google attributes")
        void extractAll() {
            Map<String, Object> attrs = Map.of(
                    "sub", "g-123",
                    "name", "Google User",
                    "email", "google@test.com",
                    "picture", "https://pic.google.com/photo.jpg"
            );
            GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(attrs);

            assertThat(info.getId()).isEqualTo("g-123");
            assertThat(info.getName()).isEqualTo("Google User");
            assertThat(info.getEmail()).isEqualTo("google@test.com");
            assertThat(info.getImageUrl()).isEqualTo("https://pic.google.com/photo.jpg");
        }

        @Test
        @DisplayName("should return null for missing attributes")
        void missingAttrs() {
            GoogleOAuth2UserInfo info = new GoogleOAuth2UserInfo(Map.of());

            assertThat(info.getId()).isNull();
            assertThat(info.getName()).isNull();
            assertThat(info.getEmail()).isNull();
            assertThat(info.getImageUrl()).isNull();
        }
    }
}

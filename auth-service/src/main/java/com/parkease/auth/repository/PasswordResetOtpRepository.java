package com.parkease.auth.repository;

import com.parkease.auth.entity.PasswordResetOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findTopByEmailIgnoreCaseAndOtpAndUsedFalseOrderByCreatedAtDesc(String email, String otp);

    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.used = true WHERE LOWER(o.email) = LOWER(:email) AND o.used = false")
    void invalidateAllByEmail(@Param("email") String email);

    Optional<PasswordResetOtp> findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE LOWER(o.email) = LOWER(:email)")
    void deleteAllByEmailIgnoreCase(@Param("email") String email);
}

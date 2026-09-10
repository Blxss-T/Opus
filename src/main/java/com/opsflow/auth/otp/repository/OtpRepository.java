package com.opsflow.auth.otp.repository;

import com.opsflow.auth.otp.domain.OtpCode;
import com.opsflow.auth.otp.domain.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(String email, OtpType type);
}

package com.opsflow.auth.otp.repository;

import com.opsflow.auth.otp.domain.OtpCode;
import com.opsflow.auth.otp.domain.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(String email, OtpType type);

    @Modifying
    @Query("UPDATE OtpCode o SET o.used = true WHERE o.email = :email AND o.type = :type AND o.used = false")
    int invalidateUnusedCodes(@Param("email") String email, @Param("type") OtpType type);
}

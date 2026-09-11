package com.opsflow.auth.otp.domain;

import com.opsflow.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "otp_codes")
public class OtpCode extends BaseEntity {

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OtpType type = OtpType.EMAIL_VERIFICATION;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    public OtpCode() {
    }

    public OtpCode(String email, String codeHash, OtpType type, Instant expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.type = type != null ? type : OtpType.EMAIL_VERIFICATION;
        this.expiresAt = expiresAt;
        this.used = false;
        this.attemptCount = 0;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public OtpType getType() {
        return type;
    }

    public void setType(OtpType type) {
        this.type = type;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }
}

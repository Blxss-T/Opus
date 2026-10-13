package com.opsflow.auth.password.controller;

import com.opsflow.auth.dto.ForgotPasswordRequest;
import com.opsflow.auth.dto.MessageResponse;
import com.opsflow.auth.dto.ResetPasswordRequest;
import com.opsflow.auth.password.PasswordResetService;
import com.opsflow.common.mail.EmailDeliveryException;
import com.opsflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
@Tag(name = "Password Reset", description = "Self-service password reset via emailed OTP code")
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot")
    @Operation(summary = "Request Password Reset", description = "Sends a password reset code to the given email. Always responds successfully to avoid revealing whether the account exists.")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.requestReset(request.email());
        } catch (EmailDeliveryException ex) {
            // Surfacing this would reveal that the account exists, so it is
            // logged for operators and the response stays identical.
            log.error("Password reset email could not be delivered: ", ex);
        }
        return ResponseEntity.ok(ApiResponse.success(
            new MessageResponse("If an account exists for this email, a password reset code has been sent.")
        ));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset Password", description = "Resets the password using the emailed reset code. All existing sessions are revoked.")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(
            new MessageResponse("Password has been reset successfully. Please sign in again.")
        ));
    }
}

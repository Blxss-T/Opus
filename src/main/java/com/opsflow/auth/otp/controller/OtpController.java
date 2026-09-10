package com.opsflow.auth.otp.controller;

import com.opsflow.auth.otp.domain.OtpCode;
import com.opsflow.auth.otp.dto.OtpResponse;
import com.opsflow.auth.otp.dto.SendOtpRequest;
import com.opsflow.auth.otp.dto.VerifyOtpRequest;
import com.opsflow.auth.otp.service.OtpService;
import com.opsflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/otp")
@Tag(name = "OTP Authentication", description = "Endpoints for sending and verifying email OTP verification codes")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send OTP Verification Code", description = "Generates and emails a 6-digit OTP code to the requested address.")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        OtpCode otpCode = otpService.sendOtp(request.email(), request.type());
        OtpResponse response = new OtpResponse(true, "OTP code dispatched successfully", otpCode.getExpiresAt());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP Code", description = "Verifies the submitted 6-digit OTP code.")
    public ResponseEntity<ApiResponse<OtpResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean valid = otpService.verifyOtp(request.email(), request.code(), request.type());
        OtpResponse response = new OtpResponse(valid, "OTP code verified successfully", null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

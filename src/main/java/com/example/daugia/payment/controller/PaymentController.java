package com.example.daugia.payment.controller;

import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.payment.dto.PaymentResponse;
import com.example.daugia.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Auction payment processing")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/auction/{auctionId}/create")
    @PreAuthorize("hasRole('BIDDER')")
    @Operation(summary = "Create VNPAY payment URL for auction winner")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@PathVariable Long auctionId,
                                                                      @AuthenticationPrincipal Jwt jwt,
                                                                      HttpServletRequest request) {
        String paymentUrl = paymentService.createPaymentUrl(auctionId, jwt.getSubject(), request);
        PaymentResponse payment = paymentService.getByAuction(auctionId, jwt.getSubject());
        PaymentResponse response = PaymentResponse.builder()
                .auctionId(payment.getAuctionId())
                .payerEmail(payment.getPayerEmail())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentUrl(paymentUrl)
                .vnpayTransactionNo(payment.getVnpayTransactionNo())
                .paidAt(payment.getPaidAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Payment URL created", response));
    }

    @GetMapping("/auction/{auctionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment details by auction")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByAuction(@PathVariable Long auctionId,
                                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Payment fetched",
                paymentService.getByAuction(auctionId, jwt.getSubject())));
    }

    @GetMapping("/vnpay-return")
    @Operation(summary = "Handle VNPAY return callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> vnpayReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(ApiResponse.success("Payment callback handled",
                paymentService.handleCallback(params)));
    }
}

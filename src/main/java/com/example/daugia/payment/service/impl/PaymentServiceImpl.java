package com.example.daugia.payment.service.impl;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.common.event.PaymentCompletedEvent;
import com.example.daugia.common.exception.AppException;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.payment.config.VnpayProperties;
import com.example.daugia.payment.dto.PaymentResponse;
import com.example.daugia.payment.entity.Payment;
import com.example.daugia.payment.entity.PaymentStatus;
import com.example.daugia.payment.repository.PaymentRepository;
import com.example.daugia.payment.service.PaymentService;
import com.example.daugia.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditJsonUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String VNP_CURRENCY = "VND";
    private static final String VNP_LOCALE = "vn";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    private final AuctionRepository auctionRepository;
    private final PaymentRepository paymentRepository;
    private final VnpayProperties vnpayProperties;
    private final DomainEventPublisher eventPublisher;
    private final AuditService auditService;

    @Override
    @Transactional
    public String createPaymentUrl(String auctionId, String winnerEmail, HttpServletRequest request) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        if (auction.getStatus() != AuctionStatus.ENDED) {
            throw new AppException("Auction is not ended", HttpStatus.BAD_REQUEST);
        }
        User winner = auction.getCurrentWinner();
        if (winner == null || winner.getEmail() == null) {
            throw new AppException("Auction does not have a winner", HttpStatus.BAD_REQUEST);
        }
        if (!winner.getEmail().equalsIgnoreCase(winnerEmail)) {
            throw new AppException("You are not the auction winner", HttpStatus.FORBIDDEN);
        }

        BigDecimal amount = auction.getCurrentPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Winning amount is invalid", HttpStatus.BAD_REQUEST);
        }

        // Check for existing pending payment to make this endpoint idempotent
        var existing = paymentRepository.findFirstByAuctionIdAndStatusOrderByCreatedAtDesc(auctionId, PaymentStatus.PENDING);
        String ipAddress = resolveClientIp(request);
        if (existing.isPresent()) {
            log.info("Reusing existing pending payment: auctionId={} txnRef={}", auctionId, existing.get().getVnpayTxnRef());
            return buildUrl(existing.get(), ipAddress, request);
        }

        String txnRef = generateTxnRef();
        Payment payment = paymentRepository.save(Payment.builder()
            .auction(auction)
            .payer(winner)
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .vnpayTxnRef(txnRef)
            .build());

        return buildUrl(payment, ipAddress, request);
    }

        private String buildUrl(Payment payment, String ipAddress, HttpServletRequest request) {
        String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", VNP_VERSION);
        params.put("vnp_Command", VNP_COMMAND);
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        params.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("vnp_CurrCode", VNP_CURRENCY);
        params.put("vnp_TxnRef", payment.getVnpayTxnRef());
        params.put("vnp_OrderInfo", "Payment for auction " + payment.getAuction().getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", VNP_LOCALE);
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", createDate);

        String expireDate = LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        params.put("vnp_ExpireDate", expireDate);

        String query = buildQueryString(params);
        String secureHash = hmacSHA512(vnpayProperties.getHashSecret(), query);

        String paymentUrl = vnpayProperties.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
        log.info("Payment URL built: auctionId={} txnRef={}", payment.getAuction().getId(), payment.getVnpayTxnRef());
        auditService.log(payment.getPayer().getEmail(), AuditAction.PAYMENT_INITIATED, "PAYMENT", payment.getId(),
            AuditOutcome.SUCCESS, request, AuditJsonUtils.toJson("txnRef", payment.getVnpayTxnRef(), "amount", payment.getAmount()));
        return paymentUrl;
        }

    @Override
    @Transactional
    public PaymentResponse handleCallback(Map<String, String> params) {
        try {
            String txnRef = params.get("vnp_TxnRef");
            if (txnRef == null || txnRef.isBlank()) {
                throw new AppException("Missing vnp_TxnRef", HttpStatus.BAD_REQUEST);
            }

            Payment payment = paymentRepository.findByVnpayTxnRef(txnRef)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            String secureHash = params.get("vnp_SecureHash");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");
            
            log.info("Payment callback received: txnRef={} responseCode={}", txnRef, responseCode);

            Map<String, String> signedParams = params.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                    .filter(entry -> !"vnp_SecureHashType".equals(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            String hashData = buildQueryString(signedParams);
            String computedHash = hmacSHA512(vnpayProperties.getHashSecret(), hashData);

            payment.setVnpayResponseCode(responseCode);

            if (secureHash == null || !secureHash.equalsIgnoreCase(computedHash) || !"00".equals(responseCode)) {
                String reason = (secureHash == null || !secureHash.equalsIgnoreCase(computedHash)) ? "Hash mismatch" : "Failed response code " + responseCode;
                log.warn("Payment callback failed: txnRef={} reason={}", txnRef, reason);
                if (payment.getStatus() != PaymentStatus.PAID) {
                    payment.setStatus(PaymentStatus.FAILED);
                }
                paymentRepository.save(payment);
                auditService.log(payment.getPayer().getEmail(), AuditAction.PAYMENT_FAILED, "PAYMENT", payment.getId(),
                        AuditOutcome.FAILURE, null, AuditJsonUtils.toJson("txnRef", txnRef, "reason", reason));
                return toResponse(payment, null);
            }

            if (payment.getStatus() != PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setVnpayTransactionNo(transactionNo);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);

                eventPublisher.publish(new PaymentCompletedEvent(
                        payment.getAuction().getId(),
                        payment.getPayer().getEmail(),
                        payment.getAmount()));
                auditService.log(payment.getPayer().getEmail(), AuditAction.PAYMENT_SUCCEEDED, "PAYMENT", payment.getId(),
                        AuditOutcome.SUCCESS, null, AuditJsonUtils.toJson("txnRef", txnRef, "transactionNo", transactionNo));
            }

            return toResponse(payment, null);
        } catch (Exception ex) {
            log.error("Unexpected exception in payment handleCallback: ", ex);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByAuction(String auctionId, String userEmail) {
        Payment payment = paymentRepository.findFirstByAuctionIdOrderByCreatedAtDesc(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getPayer() == null || payment.getPayer().getEmail() == null
                || !payment.getPayer().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AppException("You are not allowed to access this payment", HttpStatus.FORBIDDEN);
        }
        return toResponse(payment, null);
    }

    private PaymentResponse toResponse(Payment payment, String paymentUrl) {
        return PaymentResponse.builder()
                .auctionId(payment.getAuction().getId())
                .payerEmail(payment.getPayer().getEmail())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentUrl(paymentUrl)
                .vnpayTransactionNo(payment.getVnpayTransactionNo())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }


    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSHA512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            hmac.init(key);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new AppException("Failed to sign VNPAY request", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateTxnRef() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }
}

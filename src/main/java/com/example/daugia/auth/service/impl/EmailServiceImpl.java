package com.example.daugia.auth.service.impl;

import com.example.daugia.common.exception.EmailSendingException;
import com.example.daugia.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendEmail(String to, String subject, String content) {
        sendHtmlEmail(to, subject, content);
    }

    @Async
    @Override
    public void sendOtpEmail(String to, String name, String otp, String purpose) {
        String subject = "SmartAuction - Your OTP Code";
        String content = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<p>Hi " + name + ",</p>"
                + "<p>Your OTP for " + purpose + " is:</p>"
                + "<div style='font-size:32px;font-weight:bold;letter-spacing:8px;padding:16px 20px;background:#f4f6f8;display:inline-block;border-radius:10px;'>"
                + otp + "</div>"
                + "<p style='margin-top:16px'>This code expires in 10 minutes.</p>"
                + "<p>If you did not request this, you can ignore this email.</p>"
                + "</div>";
        sendHtmlEmail(to, subject, content);
    }

    @Async
    @Override
    public void sendAuctionApprovedEmail(String to, String sellerName, String productName, LocalDateTime startTime) {
        String formattedTime = startTime != null
                ? startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "N/A";
        String subject = "SmartAuction - Your Auction Has Been Approved!";
        String content = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2 style='color:#27ae60'>🎉 Auction Approved</h2>"
                + "<p>Hi " + sellerName + ",</p>"
                + "<p>Great news! Your auction for <strong>" + productName + "</strong> has been <strong style='color:#27ae60'>approved</strong>.</p>"
                + "<p>Bidding will begin at: <strong>" + formattedTime + "</strong></p>"
                + "<p>Buyers will be able to find and bid on your item once bidding opens.</p>"
                + "<p>Thank you for using SmartAuction!</p>"
                + "</div>";
        sendHtmlEmail(to, subject, content);
    }

    @Async
    @Override
    public void sendAuctionRejectedEmail(String to, String sellerName, String productName, String reason) {
        String subject = "SmartAuction - Your Auction Was Not Approved";
        String content = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2 style='color:#e74c3c'>❌ Auction Rejected</h2>"
                + "<p>Hi " + sellerName + ",</p>"
                + "<p>Unfortunately, your auction for <strong>" + productName + "</strong> was <strong style='color:#e74c3c'>rejected</strong>.</p>"
                + "<p><strong>Reason:</strong> " + reason + "</p>"
                + "<p>Please create a new auction addressing the issue above. Rejected auctions cannot be resubmitted.</p>"
                + "<p>If you believe this is an error, please contact our support team.</p>"
                + "<p>Thank you for using SmartAuction.</p>"
                + "</div>";
        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send email", e);
        }
    }
}

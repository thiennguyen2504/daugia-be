package com.example.daugia.auth.service.impl;

import com.example.daugia.common.exception.EmailSendingException;
import com.example.daugia.auth.service.EmailService;
import com.example.daugia.common.utils.LogSanitizer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            sendHtmlEmail(to, subject, content);
            log.debug("Email sent: to={} subject={}", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}", LogSanitizer.maskEmail(to), e);
            throw e;
        }
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
                + "<p style='margin-top:16px'>This code expires in 3 minutes.</p>"
                + "<p>If you did not request this, you can ignore this email.</p>"
                + "</div>";
        try {
            sendHtmlEmail(to, subject, content);
            log.debug("Email sent: to={} subject={}", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", LogSanitizer.maskEmail(to), e);
            throw e;
        }
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
        try {
            sendHtmlEmail(to, subject, content);
            log.debug("Email sent: to={} subject={}", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("Failed to send Auction Approved email to {}", LogSanitizer.maskEmail(to), e);
            throw e;
        }
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
        try {
            sendHtmlEmail(to, subject, content);
            log.debug("Email sent: to={} subject={}", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("Failed to send Auction Rejected email to {}", LogSanitizer.maskEmail(to), e);
            throw e;
        }
    }

    @Async
    @Override
    public void sendAuctionWinnerEmail(String to, String winnerName, String productName) {
        String subject = "SmartAuction - You won!";
        String content = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2>You won!</h2>"
                + "<p>Hi " + winnerName + ",</p>"
                + "<p>Your bid won the auction for <strong>" + productName + "</strong>.</p>"
                + "</div>";
        try {
            sendHtmlEmail(to, subject, content);
            log.debug("Email sent: to={} subject={}", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("Failed to send Auction Winner email to {}", LogSanitizer.maskEmail(to), e);
            throw e;
        }
    }

    @Async
    @Override
    public void sendAuctionSoldEmail(String to, String sellerName, String productName) {
        String subject = "SmartAuction - Your item sold!";
        String content = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2>Your item sold!</h2>"
                + "<p>Hi " + sellerName + ",</p>"
                + "<p>Your auction for <strong>" + productName + "</strong> has ended with a winning bid.</p>"
                + "</div>";
        try {
            sendHtmlEmail(to, subject, content);
            log.debug("Email sent: to={} subject={}", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("Failed to send Auction Sold email to {}", LogSanitizer.maskEmail(to), e);
            throw e;
        }
    }

    @Async
    @Override
    public void sendFeedbackResolvedEmail(String to, String name, String response) {
        sendNotificationEmail(to, "SmartAuction - Your feedback has been resolved",
                buildReplyEmail("Feedback Resolved", "#27ae60", name, response, "resolved"));
    }

    @Async
    @Override
    public void sendFeedbackRejectedEmail(String to, String name, String response) {
        sendNotificationEmail(to, "SmartAuction - Your feedback has been rejected",
                buildReplyEmail("Feedback Rejected", "#e74c3c", name, response, "rejected"));
    }

    @Async
    @Override
    public void sendContactResolvedEmail(String to, String name, String response) {
        sendNotificationEmail(to, "SmartAuction - Your contact message has been resolved",
                buildReplyEmail("Contact Message Resolved", "#27ae60", name, response, "resolved"));
    }

    @Async
    @Override
    public void sendContactRejectedEmail(String to, String name, String response) {
        sendNotificationEmail(to, "SmartAuction - Your contact message has been rejected",
                buildReplyEmail("Contact Message Rejected", "#e74c3c", name, response, "rejected"));
    }

    @Async
    @Override
    public void sendAdminNewFeedbackNotification(String adminEmail, String senderName, String content) {
        String subject = "SmartAuction - New feedback submitted";
        String body = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2 style='color:#CE2029'>New Feedback Submitted</h2>"
                + "<p><strong>Sender:</strong> " + senderName + "</p>"
                + "<p><strong>Content:</strong></p>"
                + "<div style='padding:16px;background:#f8f8f8;border-radius:12px'>" + content + "</div>"
                + "</div>";
        sendNotificationEmail(adminEmail, subject, body);
    }

    @Async
    @Override
    public void sendAdminNewContactNotification(String adminEmail, String senderName, String message) {
        String subject = "SmartAuction - New contact message submitted";
        String body = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2 style='color:#CE2029'>New Contact Message</h2>"
                + "<p><strong>Sender:</strong> " + senderName + "</p>"
                + "<p><strong>Message:</strong></p>"
                + "<div style='padding:16px;background:#f8f8f8;border-radius:12px'>" + message + "</div>"
                + "</div>";
        sendNotificationEmail(adminEmail, subject, body);
    }

    @Async
    @Override
    public void sendBackupFailureAlert(List<String> adminEmails, String backupType, String errorMessage) {
        if (adminEmails == null || adminEmails.isEmpty()) {
            return;
        }
        String subject = "SmartAuction - Backup Failed";
        String body = "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2 style='color:#e74c3c'>❌ Backup Failed</h2>"
                + "<p><strong>Type:</strong> " + backupType + "</p>"
                + "<p><strong>Error:</strong> " + errorMessage + "</p>"
                + "<p>Please check the backup logs and take action.</p>"
                + "</div>";
        adminEmails.forEach(email -> sendNotificationEmail(email, subject, body));
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

    private void sendNotificationEmail(String to, String subject, String content) {
        try {
            sendHtmlEmail(to, subject, content);
            log.debug("Email sent: to={} subject={}", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}", LogSanitizer.maskEmail(to), e);
            throw e;
        }
    }

    private String buildReplyEmail(String heading, String color, String name, String response, String action) {
        return "<div style='font-family:Arial,sans-serif;line-height:1.6'>"
                + "<h2 style='color:" + color + "'>" + heading + "</h2>"
                + "<p>Hi " + name + ",</p>"
                + "<p>Your item was <strong>" + action + "</strong> by our admin team.</p>"
                + "<p><strong>Response:</strong></p>"
                + "<div style='padding:16px;background:#f8f8f8;border-radius:12px'>" + response + "</div>"
                + "<p style='margin-top:16px'>Thank you for using SmartAuction.</p>"
                + "</div>";
    }
}

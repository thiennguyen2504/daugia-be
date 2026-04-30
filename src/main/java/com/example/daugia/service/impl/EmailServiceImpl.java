package com.example.daugia.service.impl;

import com.example.daugia.exception.EmailSendingException;
import com.example.daugia.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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

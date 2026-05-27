package com.example.daugia.feedback.service.impl;

import com.example.daugia.auth.service.EmailService;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.common.exception.AppException;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.feedback.dto.FeedbackCreateRequest;
import com.example.daugia.feedback.dto.FeedbackReplyRequest;
import com.example.daugia.feedback.dto.FeedbackResponse;
import com.example.daugia.feedback.entity.Feedback;
import com.example.daugia.feedback.entity.FeedbackStatus;
import com.example.daugia.feedback.mapper.FeedbackMapper;
import com.example.daugia.feedback.repository.FeedbackRepository;
import com.example.daugia.feedback.service.FeedbackService;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${spring.mail.username:}")
    private String adminNotificationEmail;

    @Override
    @Transactional
    public FeedbackResponse submit(FeedbackCreateRequest request, String userEmail) {
        String role = "GUEST";
        if (userEmail != null && !userEmail.isBlank()) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null && user.getRole() != null && user.getRole().getName() != null) {
                role = user.getRole().getName();
            }
        }

        Feedback feedback = Feedback.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(role)
                .content(request.getContent())
                .status(FeedbackStatus.PENDING)
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        if (adminNotificationEmail != null && !adminNotificationEmail.isBlank()) {
            emailService.sendAdminNewFeedbackNotification(adminNotificationEmail, saved.getFullName(), saved.getContent());
        }
        return feedbackMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponse> getAll(FeedbackStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> resultPage = status == null
                ? feedbackRepository.findAll(pageable)
                : feedbackRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        return PageResponse.from(resultPage.map(feedbackMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getById(String id) {
        return feedbackMapper.toResponse(findFeedback(id));
    }

    @Override
    @Transactional
    public FeedbackResponse resolve(String id, FeedbackReplyRequest request, String adminEmail) {
        if (request.getApprove() != null && !Boolean.TRUE.equals(request.getApprove())) {
            throw new AppException("Approve flag must be true when resolving", HttpStatus.BAD_REQUEST);
        }
        Feedback feedback = findFeedback(id);
        if (feedback.getStatus() != FeedbackStatus.PENDING) {
            throw new AppException("Feedback is not pending review", HttpStatus.BAD_REQUEST);
        }

        feedback.setStatus(FeedbackStatus.RESOLVED);
        feedback.setResponse(request.getResponse());
        feedback.setRespondedBy(adminEmail);
        feedback.setRespondedAt(LocalDateTime.now());

        Feedback saved = feedbackRepository.save(feedback);
        auditService.log(adminEmail, AuditAction.FEEDBACK_RESOLVED, "FEEDBACK", saved.getId(),
                AuditOutcome.SUCCESS, "Feedback resolved");
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            emailService.sendFeedbackResolvedEmail(saved.getEmail(), saved.getFullName(), saved.getResponse());
        }
        return feedbackMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FeedbackResponse reject(String id, FeedbackReplyRequest request, String adminEmail) {
        if (request.getApprove() != null && Boolean.TRUE.equals(request.getApprove())) {
            throw new AppException("Approve flag must be false when rejecting", HttpStatus.BAD_REQUEST);
        }
        Feedback feedback = findFeedback(id);
        if (feedback.getStatus() != FeedbackStatus.PENDING) {
            throw new AppException("Feedback is not pending review", HttpStatus.BAD_REQUEST);
        }

        feedback.setStatus(FeedbackStatus.REJECTED);
        feedback.setResponse(request.getResponse());
        feedback.setRespondedBy(adminEmail);
        feedback.setRespondedAt(LocalDateTime.now());

        Feedback saved = feedbackRepository.save(feedback);
        auditService.log(adminEmail, AuditAction.FEEDBACK_REJECTED, "FEEDBACK", saved.getId(),
                AuditOutcome.SUCCESS, "Feedback rejected");
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            emailService.sendFeedbackRejectedEmail(saved.getEmail(), saved.getFullName(), saved.getResponse());
        }
        return feedbackMapper.toResponse(saved);
    }

    private Feedback findFeedback(String id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + id));
    }
}
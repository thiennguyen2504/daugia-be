package com.example.daugia.feedback.service.impl;

import com.example.daugia.auth.service.EmailService;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.common.exception.AppException;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.feedback.dto.ContactMessageCreateRequest;
import com.example.daugia.feedback.dto.ContactMessageReplyRequest;
import com.example.daugia.feedback.dto.ContactMessageResponse;
import com.example.daugia.feedback.entity.ContactMessage;
import com.example.daugia.feedback.entity.ContactStatus;
import com.example.daugia.feedback.mapper.ContactMessageMapper;
import com.example.daugia.feedback.repository.ContactMessageRepository;
import com.example.daugia.feedback.service.ContactMessageService;
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
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final ContactMessageMapper contactMessageMapper;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${spring.mail.username:}")
    private String adminNotificationEmail;

    @Override
    @Transactional
    public ContactMessageResponse submit(ContactMessageCreateRequest request) {
        ContactMessage contactMessage = ContactMessage.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .message(request.getMessage())
                .status(ContactStatus.PENDING)
                .build();

        ContactMessage saved = contactMessageRepository.save(contactMessage);
        if (adminNotificationEmail != null && !adminNotificationEmail.isBlank()) {
            emailService.sendAdminNewContactNotification(adminNotificationEmail, saved.getFullName(), saved.getMessage());
        }
        return contactMessageMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> getAll(ContactStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ContactMessage> resultPage = status == null
                ? contactMessageRepository.findAll(pageable)
                : contactMessageRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        return PageResponse.from(resultPage.map(contactMessageMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ContactMessageResponse getById(String id) {
        return contactMessageMapper.toResponse(findContactMessage(id));
    }

    @Override
    @Transactional
    public ContactMessageResponse resolve(String id, ContactMessageReplyRequest request, String adminEmail) {
        if (request.getApprove() != null && !Boolean.TRUE.equals(request.getApprove())) {
            throw new AppException("Approve flag must be true when resolving", HttpStatus.BAD_REQUEST);
        }
        ContactMessage contactMessage = findContactMessage(id);
        if (contactMessage.getStatus() != ContactStatus.PENDING) {
            throw new AppException("Contact message is not pending review", HttpStatus.BAD_REQUEST);
        }

        contactMessage.setStatus(ContactStatus.RESOLVED);
        contactMessage.setResponse(request.getResponse());
        contactMessage.setRespondedBy(adminEmail);
        contactMessage.setRespondedAt(LocalDateTime.now());

        ContactMessage saved = contactMessageRepository.save(contactMessage);
        auditService.log(adminEmail, AuditAction.CONTACT_RESOLVED, "CONTACT_MESSAGE", saved.getId(),
                AuditOutcome.SUCCESS, "Contact message resolved");
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            emailService.sendContactResolvedEmail(saved.getEmail(), saved.getFullName(), saved.getResponse());
        }
        return contactMessageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ContactMessageResponse reject(String id, ContactMessageReplyRequest request, String adminEmail) {
        if (request.getApprove() != null && Boolean.TRUE.equals(request.getApprove())) {
            throw new AppException("Approve flag must be false when rejecting", HttpStatus.BAD_REQUEST);
        }
        ContactMessage contactMessage = findContactMessage(id);
        if (contactMessage.getStatus() != ContactStatus.PENDING) {
            throw new AppException("Contact message is not pending review", HttpStatus.BAD_REQUEST);
        }

        contactMessage.setStatus(ContactStatus.REJECTED);
        contactMessage.setResponse(request.getResponse());
        contactMessage.setRespondedBy(adminEmail);
        contactMessage.setRespondedAt(LocalDateTime.now());

        ContactMessage saved = contactMessageRepository.save(contactMessage);
        auditService.log(adminEmail, AuditAction.CONTACT_REJECTED, "CONTACT_MESSAGE", saved.getId(),
                AuditOutcome.SUCCESS, "Contact message rejected");
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            emailService.sendContactRejectedEmail(saved.getEmail(), saved.getFullName(), saved.getResponse());
        }
        return contactMessageMapper.toResponse(saved);
    }

    private ContactMessage findContactMessage(String id) {
        return contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found: " + id));
    }
}
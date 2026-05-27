package com.example.daugia.feedback.repository;

import com.example.daugia.feedback.entity.ContactMessage;
import com.example.daugia.feedback.entity.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, String>, JpaSpecificationExecutor<ContactMessage> {
    Page<ContactMessage> findAllByStatusOrderByCreatedAtDesc(ContactStatus status, Pageable pageable);
}
package com.example.daugia.feedback.repository;

import com.example.daugia.feedback.entity.Feedback;
import com.example.daugia.feedback.entity.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FeedbackRepository extends JpaRepository<Feedback, String>, JpaSpecificationExecutor<Feedback> {
    Page<Feedback> findAllByStatusOrderByCreatedAtDesc(FeedbackStatus status, Pageable pageable);
}
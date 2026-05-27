package com.example.daugia.feedback.service;

import com.example.daugia.feedback.dto.FeedbackCreateRequest;
import com.example.daugia.feedback.dto.FeedbackReplyRequest;
import com.example.daugia.feedback.dto.FeedbackResponse;
import com.example.daugia.feedback.entity.FeedbackStatus;
import com.example.daugia.common.dto.PageResponse;

public interface FeedbackService {
    FeedbackResponse submit(FeedbackCreateRequest request, String userEmail);
    PageResponse<FeedbackResponse> getAll(FeedbackStatus status, int page, int size);
    FeedbackResponse getById(String id);
    FeedbackResponse resolve(String id, FeedbackReplyRequest request, String adminEmail);
    FeedbackResponse reject(String id, FeedbackReplyRequest request, String adminEmail);
}
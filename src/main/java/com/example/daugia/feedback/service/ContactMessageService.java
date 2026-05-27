package com.example.daugia.feedback.service;

import com.example.daugia.feedback.dto.ContactMessageCreateRequest;
import com.example.daugia.feedback.dto.ContactMessageReplyRequest;
import com.example.daugia.feedback.dto.ContactMessageResponse;
import com.example.daugia.feedback.entity.ContactStatus;
import com.example.daugia.common.dto.PageResponse;

public interface ContactMessageService {
    ContactMessageResponse submit(ContactMessageCreateRequest request);
    PageResponse<ContactMessageResponse> getAll(ContactStatus status, int page, int size);
    ContactMessageResponse getById(String id);
    ContactMessageResponse resolve(String id, ContactMessageReplyRequest request, String adminEmail);
    ContactMessageResponse reject(String id, ContactMessageReplyRequest request, String adminEmail);
}
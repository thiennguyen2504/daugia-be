package com.example.daugia.feedback.mapper;

import com.example.daugia.feedback.dto.ContactMessageResponse;
import com.example.daugia.feedback.entity.ContactMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContactMessageMapper {
    ContactMessageResponse toResponse(ContactMessage contactMessage);
}
package com.example.daugia.feedback.mapper;

import com.example.daugia.feedback.dto.FeedbackResponse;
import com.example.daugia.feedback.entity.Feedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {
    FeedbackResponse toResponse(Feedback feedback);
}
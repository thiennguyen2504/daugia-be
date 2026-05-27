package com.example.daugia.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreResponse {
    private String restoreId;
    private RestoreStatus status;
    private String message;
    private Long estimatedDurationMs;
}

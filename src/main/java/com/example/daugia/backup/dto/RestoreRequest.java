package com.example.daugia.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreRequest {
    private String backupId;
    private LocalDateTime targetDateTime;
    private String confirmedBy;
}

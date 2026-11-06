package dev.routex.management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor

public class HealthResponse {
    private String serviceStatus;
    private String dbStatus;
    private LocalDateTime timestamp;
}

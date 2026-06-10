package com.gurjeet.pm.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Object details, String correlationId, Instant timestamp) {
    public static ErrorResponse of(String code, String message, Object details, String correlationId) {
        return new ErrorResponse(code, message, details, correlationId, Instant.now());
    }
}

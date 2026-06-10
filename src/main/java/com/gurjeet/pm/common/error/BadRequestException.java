package com.gurjeet.pm.common.error;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, null);
    }
    public BadRequestException(String message, Object details) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, details);
    }
}

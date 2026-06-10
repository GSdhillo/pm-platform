package com.gurjeet.pm.common.error;

import org.springframework.http.HttpStatus;

public class UnprocessableException extends ApiException {
    public UnprocessableException(String code, String message, Object details) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message, details);
    }
}

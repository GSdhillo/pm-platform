package com.gurjeet.pm.common.error;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final transient Object details;

    protected ApiException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Object details() { return details; }
}

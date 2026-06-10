package com.gurjeet.pm.common.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
    public ConflictException(String code, String message, Object details) {
        super(HttpStatus.CONFLICT, code, message, details);
    }
    public static ConflictException versionConflict(long currentVersion, Object currentState) {
        return new ConflictException("VERSION_CONFLICT",
                "Issue was modified by another user. Re-fetch, merge your changes, and retry with the current version.",
                new VersionConflictDetails(currentVersion, currentState));
    }
    public record VersionConflictDetails(long currentVersion, Object currentState) {}
}

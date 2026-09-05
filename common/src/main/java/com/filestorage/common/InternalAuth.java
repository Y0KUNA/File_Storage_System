package com.filestorage.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class InternalAuth {
    private InternalAuth() {
    }

    public static void requireToken(String provided, String expected) {
        if (expected == null || expected.isBlank() || !expected.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "INVALID_INTERNAL_TOKEN");
        }
    }
}


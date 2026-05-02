package com.smartcampus.exception;

/**
 * Thrown when a client references a related resource (e.g. a {@code roomId} inside a
 * Sensor payload) that does not exist in the system.
 *
 * <p>Mapped to HTTP 422 Unprocessable Entity — the request body is syntactically valid
 * JSON but semantically invalid because a required dependency is missing.</p>
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}

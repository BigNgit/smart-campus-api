package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps {@link LinkedResourceNotFoundException} to HTTP 422 Unprocessable Entity.
 *
 * <p>422 is semantically more accurate than 404 here because the request URL is valid
 * and the JSON payload is syntactically correct — the problem is that a referenced
 * entity (e.g. the roomId inside a Sensor body) does not exist.  A 404 would suggest
 * the endpoint itself was not found, which is misleading.</p>
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    // HTTP 422 is not in the javax.ws.rs.core.Response.Status enum for older JAX-RS versions,
    // so we reference it by its integer code directly.
    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ErrorResponse error = new ErrorResponse(
                UNPROCESSABLE_ENTITY,
                "Unprocessable Entity",
                exception.getMessage()
        );
        return Response
                .status(UNPROCESSABLE_ENTITY)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

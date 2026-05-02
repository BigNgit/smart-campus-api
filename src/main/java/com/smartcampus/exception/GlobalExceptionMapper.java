package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "catch-all" exception mapper — the API's safety net.
 *
 * <p>Intercepts any {@link Throwable} not handled by a more specific mapper
 * (e.g. {@link NullPointerException}, {@link IndexOutOfBoundsException}) and returns
 * a generic HTTP 500 response WITHOUT exposing internal stack traces.</p>
 *
 * <p><b>Cybersecurity rationale (Report Q4):</b> Exposing raw Java stack traces leaks:
 * <ul>
 *   <li>Internal package/class names — revealing the technology stack and framework versions
 *       that can be cross-referenced with known CVEs.</li>
 *   <li>File system paths — showing the server's directory layout, useful for path-traversal
 *       attacks.</li>
 *   <li>Database queries (if present) — exposing schema details and column names for
 *       SQL-injection reconnaissance.</li>
 *   <li>Business logic — internal method names hint at algorithms and data flows an
 *       attacker can exploit.</li>
 * </ul>
 * By returning only a generic "Internal Server Error" message, the full error detail is
 * written exclusively to the server-side log, where only authorised personnel can see it.</p>
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full stack trace on the server side (admins can see it; clients cannot).
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by GlobalExceptionMapper: "
                + exception.getMessage(), exception);

        ErrorResponse error = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred on the server. "
                        + "Please contact the administrator if the problem persists."
        );
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

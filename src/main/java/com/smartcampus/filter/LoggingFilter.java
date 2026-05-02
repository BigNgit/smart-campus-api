package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS filter for API observability.
 *
 * <p>Implements both {@link ContainerRequestFilter} and {@link ContainerResponseFilter} so
 * that a single class handles the full request/response lifecycle without polluting the
 * resource classes with logging boilerplate.</p>
 *
 * <p><b>Why filters for cross-cutting concerns (Report Q5):</b>
 * Using a dedicated filter separates the logging concern from business logic, adhering to
 * the Single-Responsibility Principle.  If logging needs to change (e.g. switch to a
 * different format or add correlation IDs) only one class needs updating rather than
 * every resource method.  Filters are also guaranteed to run for every request — including
 * error paths — so no request is accidentally left unlogged.  Manual {@code Logger.info()}
 * calls inside resource methods, on the other hand, are easily forgotten, duplicated, or
 * inconsistently formatted, and they mix infrastructure code with domain logic.</p>
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Logs every incoming HTTP request: method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Logs every outgoing HTTP response: final status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}

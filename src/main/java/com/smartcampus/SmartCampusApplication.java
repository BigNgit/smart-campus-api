package com.smartcampus;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * JAX-RS Application configuration class.
 *
 * <p>The {@code @ApplicationPath} annotation establishes the versioned API entry point at
 * {@code /api/v1}.  When deployed on Grizzly the base URI is set directly in
 * {@link Main#BASE_URI}, but the annotation is kept here for completeness and
 * compatibility with servlet-based deployments.</p>
 *
 * <p><b>Default lifecycle note (Report Q1):</b> By default JAX-RS creates a new instance of
 * each resource class per request (request-scoped). This means instance fields are NOT
 * shared across requests, so shared mutable state (like the in-memory maps) must live in a
 * singleton ({@link com.smartcampus.store.DataStore}) and be accessed via thread-safe
 * data structures ({@code ConcurrentHashMap}) to prevent race conditions and data loss.</p>
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Auto-scan all resource, exception-mapper and filter classes in these packages
        packages(
                "com.smartcampus.resource",
                "com.smartcampus.exception",
                "com.smartcampus.filter"
        );
        // Enable Jackson for automatic JSON serialisation / deserialisation
        register(JacksonFeature.class);
    }
}

package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Entry point for the Smart Campus API.
 * Starts an embedded Grizzly HTTP server (no Tomcat / WAR deployment needed).
 * Server listens on http://0.0.0.0:8080/api/v1/
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /** Full base URI served by Grizzly. All resource paths are relative to this. */
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    /**
     * Creates and starts the Grizzly HTTP server exposing the JAX-RS resources.
     *
     * @return a running {@link HttpServer} instance
     */
    public static HttpServer startServer() {
        SmartCampusApplication config = new SmartCampusApplication();
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();

        LOGGER.info("===========================================================");
        LOGGER.info("  Smart Campus API started successfully.");
        LOGGER.info("  Base URL : " + BASE_URI);
        LOGGER.info("  Discovery: http://localhost:8080/api/v1/");
        LOGGER.info("  Rooms    : http://localhost:8080/api/v1/rooms");
        LOGGER.info("  Sensors  : http://localhost:8080/api/v1/sensors");
        LOGGER.info("  Press ENTER to stop the server ...");
        LOGGER.info("===========================================================");

        // Block until the user presses Enter, then shut down gracefully.
        System.in.read();
        server.shutdownNow();
        LOGGER.info("Server stopped.");
    }
}

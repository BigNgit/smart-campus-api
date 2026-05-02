package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery / root endpoint at {@code GET /api/v1/}.
 *
 * <p>Returns essential API metadata: version, contact, and a hypermedia map of primary
 * resource collections.</p>
 *
 * <p><b>HATEOAS rationale (Report Q1.2):</b>
 * Hypermedia As The Engine Of Application State (HATEOAS) embeds navigable links inside
 * API responses so that clients can discover available actions dynamically, without
 * consulting out-of-date documentation.  This reduces coupling between client and server:
 * if a URL changes, the server updates the link in the response and all well-written
 * clients automatically adapt.  Static documentation, by contrast, becomes stale the moment
 * a refactor happens and requires coordinated updates across teams.  HATEOAS also enables
 * exploratory API usage — a new client developer can start at the root endpoint and
 * follow links to understand the entire API surface, much like browsing a website.</p>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("apiName", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description",
                "RESTful API for managing campus rooms and IoT sensors built with JAX-RS / Jersey on Grizzly.");
        response.put("contact", "admin@smartcampus.westminster.ac.uk");

        // Hypermedia links — HATEOAS: clients can navigate the API from this single entry point
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1/");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("resources", links);

        Map<String, String> actions = new LinkedHashMap<>();
        actions.put("listRooms",      "GET    /api/v1/rooms");
        actions.put("createRoom",     "POST   /api/v1/rooms");
        actions.put("getRoom",        "GET    /api/v1/rooms/{roomId}");
        actions.put("deleteRoom",     "DELETE /api/v1/rooms/{roomId}");
        actions.put("listSensors",    "GET    /api/v1/sensors");
        actions.put("listSensorsByType","GET  /api/v1/sensors?type={type}");
        actions.put("createSensor",   "POST   /api/v1/sensors");
        actions.put("getSensor",      "GET    /api/v1/sensors/{sensorId}");
        actions.put("deleteSensor",   "DELETE /api/v1/sensors/{sensorId}");
        actions.put("listReadings",   "GET    /api/v1/sensors/{sensorId}/readings");
        actions.put("addReading",     "POST   /api/v1/sensors/{sensorId}/readings");
        response.put("availableActions", actions);

        return Response.ok(response).build();
    }
}

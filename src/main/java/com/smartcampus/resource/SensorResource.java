package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JAX-RS resource for managing Sensors at {@code /api/v1/sensors}.
 *
 * <hr>
 * <p><b>Report Q3.1 — {@code @Consumes(APPLICATION_JSON)} consequences:</b>
 * When {@code @Consumes(MediaType.APPLICATION_JSON)} is declared, JAX-RS will only invoke
 * this method if the incoming {@code Content-Type} header is {@code application/json}.
 * If a client sends {@code text/plain} or {@code application/xml}, the runtime immediately
 * rejects the request with HTTP 415 Unsupported Media Type before any resource method code
 * executes.  This protects the API from malformed or unexpected payloads and makes the
 * contract explicit: clients MUST serialise their bodies as JSON.</p>
 *
 * <hr>
 * <p><b>Report Q3.2 — {@code @QueryParam} vs path segment for filtering:</b>
 * A query parameter ({@code GET /sensors?type=CO2}) is superior for filtering/searching
 * because:
 * <ul>
 *   <li>It keeps the resource identifier ({@code /sensors}) stable — the collection URL
 *       does not change based on filter criteria.</li>
 *   <li>Query parameters are inherently optional, so {@code GET /sensors} (no filter) and
 *       {@code GET /sensors?type=CO2} (filtered) are naturally handled by the same method
 *       without separate {@code @Path} definitions.</li>
 *   <li>Multiple independent filters can be composed freely:
 *       {@code /sensors?type=CO2&status=ACTIVE}, whereas path segments would require
 *       combinatorial route definitions.</li>
 *   <li>Path segments ({@code /sensors/type/CO2}) imply a hierarchy that does not exist —
 *       "type" is not a sub-resource of "sensors"; it is a search criterion.</li>
 * </ul>
 * </p>
 */
@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ------------------------------------------------------------------ //
    //  GET /sensors[?type=X]  — list sensors, optionally filtered by type
    // ------------------------------------------------------------------ //

    /**
     * Returns all sensors.  If the optional {@code type} query parameter is provided, only
     * sensors whose {@code type} field matches (case-insensitive) are returned.
     *
     * @param type optional filter, e.g. {@code ?type=CO2}
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    // ------------------------------------------------------------------ //
    //  POST /sensors  — register a new sensor
    // ------------------------------------------------------------------ //

    /**
     * Registers a new sensor.
     *
     * <p><b>Integrity check:</b> the {@code roomId} in the request body MUST reference an
     * existing room.  If not, {@link LinkedResourceNotFoundException} is thrown → HTTP 422.</p>
     *
     * <p>Returns 201 Created with the persisted sensor object.</p>
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            throw new LinkedResourceNotFoundException("Request body must not be empty.");
        }

        // Validate that the referenced roomId actually exists
        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            throw new LinkedResourceNotFoundException("Field 'roomId' is required when registering a sensor.");
        }

        Room room = store.getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: the specified roomId '" + sensor.getRoomId()
                            + "' does not exist. Create the room first.");
        }

        // Validate required sensor fields
        if (sensor.getType() == null || sensor.getType().trim().isEmpty()) {
            throw new LinkedResourceNotFoundException("Field 'type' is required (e.g. Temperature, CO2, Occupancy).");
        }

        // Auto-generate ID if not provided
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        // Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        // Persist the sensor
        store.getSensors().put(sensor.getId(), sensor);

        // Update the room's sensorIds list so the room knows about its sensors
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // ------------------------------------------------------------------ //
    //  GET /sensors/{sensorId}  — get a specific sensor
    // ------------------------------------------------------------------ //

    /**
     * Returns detailed information about a single sensor.
     */
    @GET
    @Path("{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found with ID: '" + sensorId + "'");
        }
        return Response.ok(sensor).build();
    }

    // ------------------------------------------------------------------ //
    //  DELETE /sensors/{sensorId}  — remove a sensor
    // ------------------------------------------------------------------ //

    /**
     * Removes a sensor from the system.
     * Also removes it from its parent room's {@code sensorIds} list and clears its
     * reading history to prevent orphaned data.
     */
    @DELETE
    @Path("{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found with ID: '" + sensorId + "'");
        }

        // Remove sensor ID from its parent room's list
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }

        // Remove sensor and its reading history
        store.getSensors().remove(sensorId);
        store.getReadings().remove(sensorId);

        return Response.noContent().build();
    }

    // ------------------------------------------------------------------ //
    //  Sub-resource locator: /sensors/{sensorId}/readings
    // ------------------------------------------------------------------ //

    /**
     * Sub-resource locator for sensor readings.
     *
     * <p>Returns an instance of {@link SensorReadingResource} which JAX-RS will use to
     * handle all requests under {@code /sensors/{sensorId}/readings}.  The method does
     * NOT carry an HTTP method annotation — it is a pure locator.</p>
     *
     * <p>Validates that the sensor exists before delegating; throws 404 otherwise.</p>
     *
     * @param sensorId the sensor whose reading history is requested
     * @return a configured {@link SensorReadingResource} instance
     */
    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor exists before delegating to sub-resource
        if (!store.getSensors().containsKey(sensorId)) {
            throw new ResourceNotFoundException("Sensor not found with ID: '" + sensorId + "'");
        }
        return new SensorReadingResource(sensorId);
    }
}

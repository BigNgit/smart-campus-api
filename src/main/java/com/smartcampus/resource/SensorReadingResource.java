package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for managing the reading history of a single sensor.
 *
 * <p>This class is NOT annotated with {@code @Path} at the class level because it is
 * instantiated and returned by the sub-resource locator method in
 * {@link SensorResource}, not discovered directly by the JAX-RS runtime.</p>
 *
 * <hr>
 * <p><b>Report Q4.1 — Sub-Resource Locator pattern benefits:</b>
 * The Sub-Resource Locator pattern allows a parent resource to delegate the handling of
 * a child path to a completely separate class.  Key benefits are:
 * <ul>
 *   <li><b>Separation of concerns:</b> Reading-related logic (history, side effects, state
 *       validation) lives in its own class, keeping {@link SensorResource} focused on
 *       sensor management.</li>
 *   <li><b>Testability:</b> {@code SensorReadingResource} can be unit-tested in isolation
 *       by constructing it with a specific sensor ID, without bootstrapping the whole
 *       JAX-RS runtime.</li>
 *   <li><b>Maintainability at scale:</b> In a real campus API with dozens of nested paths
 *       (sensors/{id}/readings, sensors/{id}/alerts, sensors/{id}/calibrations …),
 *       packing every handler into one class creates an unmanageable "God class".
 *       Sub-resources distribute responsibility in a natural hierarchy that mirrors the
 *       URL structure.</li>
 *   <li><b>Reusability:</b> The sub-resource class can be reused by multiple parent
 *       locators if the same reading pattern is needed for different entity types.</li>
 * </ul>
 * </p>
 */
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    /**
     * Called by the sub-resource locator in {@link SensorResource}.
     *
     * @param sensorId the sensor whose readings this sub-resource manages
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ------------------------------------------------------------------ //
    //  GET /sensors/{sensorId}/readings  — fetch full reading history
    // ------------------------------------------------------------------ //

    /**
     * Returns all historical readings for this sensor in chronological order.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadings() {
        List<SensorReading> readings = store.getReadingsForSensor(sensorId);
        return Response.ok(readings).build();
    }

    // ------------------------------------------------------------------ //
    //  POST /sensors/{sensorId}/readings  — append a new reading
    // ------------------------------------------------------------------ //

    /**
     * Appends a new reading to this sensor's history.
     *
     * <p><b>Business rules:</b></p>
     * <ul>
     *   <li>If the sensor status is {@code "MAINTENANCE"}, the request is rejected with
     *       403 Forbidden ({@link SensorUnavailableException}).</li>
     *   <li>On success the parent {@link Sensor#setCurrentValue(double)} is updated to
     *       reflect the latest measurement, ensuring data consistency across the API.</li>
     * </ul>
     *
     * <p>Returns 201 Created with the persisted reading.</p>
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        // State constraint: MAINTENANCE sensors cannot accept readings (403)
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        // Also block OFFLINE sensors — hardware is disconnected
        if ("OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        if (reading == null) {
            reading = new SensorReading();
        }

        // Auto-generate ID if not supplied
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Auto-set timestamp to now if not supplied
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        store.getReadingsForSensor(sensorId).add(reading);

        // Side effect: update the parent sensor's currentValue for data consistency
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}

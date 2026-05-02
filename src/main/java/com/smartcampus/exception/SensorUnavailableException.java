package com.smartcampus.exception;

/**
 * Thrown when a client attempts to POST a new reading to a sensor that is currently
 * in MAINTENANCE status and therefore cannot accept new data.
 *
 * <p>Mapped to HTTP 403 Forbidden — the server understands the request but refuses
 * to process it due to the current state of the sensor.</p>
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' has status '" + status
                + "' and cannot accept new readings at this time.");
        this.sensorId = sensorId;
    }

    public String getSensorId() {
        return sensorId;
    }
}

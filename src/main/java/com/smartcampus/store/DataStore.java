package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe singleton in-memory data store.
 *
 * <p>Because JAX-RS creates a new resource-class instance per request, any shared
 * mutable state MUST live outside the resource classes.  This singleton fulfils
 * that role using {@link ConcurrentHashMap} (safe concurrent reads/writes) and
 * {@link CopyOnWriteArrayList} for the per-sensor reading history.</p>
 *
 * <p>No database technology is used — only Java collections, as required by
 * the coursework specification.</p>
 */
public class DataStore {

    // ------------------------------------------------------------------ //
    //  Singleton
    // ------------------------------------------------------------------ //

    private static final DataStore INSTANCE = new DataStore();

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------ //
    //  In-memory stores
    // ------------------------------------------------------------------ //

    /** Rooms keyed by room ID. */
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    /** Sensors keyed by sensor ID. */
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    /**
     * Sensor readings keyed by sensor ID.
     * Each sensor has its own {@link CopyOnWriteArrayList} so that concurrent
     * reads are safe without locking.
     */
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ //
    //  Accessors
    // ------------------------------------------------------------------ //

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Map<String, List<SensorReading>> getReadings() {
        return readings;
    }

    /**
     * Returns the reading list for a given sensor, creating an empty list if
     * none exists yet.  Uses {@code computeIfAbsent} which is atomic in
     * {@link ConcurrentHashMap}, preventing duplicate list creation under
     * concurrent requests.
     *
     * @param sensorId the sensor whose reading history is requested
     * @return the (possibly empty) list of readings
     */
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
    }
}

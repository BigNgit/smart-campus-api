package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

/**
 * JAX-RS resource for managing Rooms at {@code /api/v1/rooms}.
 *
 * <hr>
 * <p><b>Report Q2.1 — IDs vs Full Objects:</b>
 * Returning only IDs in a list response minimises payload size and is efficient when the
 * client needs only references (e.g. to build a dropdown).  However it forces the client to
 * issue N+1 follow-up requests to retrieve details for each room, increasing round-trip
 * latency and server load.  Returning full objects costs more bandwidth on the initial
 * request but eliminates those follow-up calls.  A pragmatic middle ground is to return
 * a "summary" projection (id + name) and let the client fetch full details via
 * {@code GET /rooms/{id}} only when needed.</p>
 *
 * <hr>
 * <p><b>Report Q2.2 — DELETE idempotency:</b>
 * HTTP defines DELETE as idempotent: repeated identical requests should have the same
 * effect on the server state.  In terms of server state this implementation IS idempotent —
 * after the first successful DELETE the room is gone, and subsequent calls leave the state
 * unchanged (room is still absent).  However, the HTTP response code changes: the first
 * call returns 204 No Content, while subsequent calls return 404 Not Found (because the
 * room no longer exists).  This diverges from a strict reading of idempotency (which could
 * argue for returning 204 every time), but returning 404 for a genuinely missing resource
 * is the more widely-adopted and semantically honest practice in modern REST APIs — the
 * client is informed that there is nothing to delete rather than receiving a silent success.</p>
 */
@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ------------------------------------------------------------------ //
    //  GET /rooms  — list all rooms
    // ------------------------------------------------------------------ //

    /**
     * Returns a comprehensive list of all rooms currently in the system.
     */
    @GET
    public Response getAllRooms() {
        return Response.ok(new ArrayList<>(store.getRooms().values())).build();
    }

    // ------------------------------------------------------------------ //
    //  POST /rooms  — create a new room
    // ------------------------------------------------------------------ //

    /**
     * Creates a new room.  If no {@code id} is supplied in the request body, a UUID is
     * generated automatically.  Returns 201 Created with the persisted room.
     */
    @POST
    public Response createRoom(Room room) {
        // Validate required fields
        if (room == null) {
            throw new LinkedResourceNotFoundException("Request body must not be empty.");
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            throw new LinkedResourceNotFoundException("Room 'name' is required.");
        }

        // Auto-generate ID if not provided
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }

        // Check for duplicate ID
        if (store.getRooms().containsKey(room.getId())) {
            throw new LinkedResourceNotFoundException(
                    "A room with ID '" + room.getId() + "' already exists.");
        }

        // Ensure sensorIds list is initialised
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.getRooms().put(room.getId(), room);

        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // ------------------------------------------------------------------ //
    //  GET /rooms/{roomId}  — get a specific room
    // ------------------------------------------------------------------ //

    /**
     * Returns detailed metadata for a single room identified by {@code roomId}.
     * Throws {@link ResourceNotFoundException} (404) if the room does not exist.
     */
    @GET
    @Path("{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found with ID: '" + roomId + "'");
        }
        return Response.ok(room).build();
    }

    // ------------------------------------------------------------------ //
    //  DELETE /rooms/{roomId}  — decommission a room
    // ------------------------------------------------------------------ //

    /**
     * Deletes a room.
     *
     * <p>Business rule: a room with at least one sensor still assigned to it CANNOT be
     * deleted.  Attempting to do so throws {@link RoomNotEmptyException} which is mapped
     * to HTTP 409 Conflict.</p>
     *
     * <p>Returns 204 No Content on success.</p>
     */
    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found with ID: '" + roomId + "'");
        }

        // Safety check — prevent data orphans
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        store.getRooms().remove(roomId);

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}

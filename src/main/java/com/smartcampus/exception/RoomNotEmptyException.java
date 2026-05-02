package com.smartcampus.exception;

/**
 * Thrown when an attempt is made to delete a Room that still has Sensors assigned to it.
 * Mapped to HTTP 409 Conflict.
 *
 * <p>Preventing orphaned sensors ensures referential integrity in the in-memory store.</p>
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' cannot be deleted because it still has active sensors assigned to it. "
                + "Please remove or reassign all sensors before decommissioning the room.");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}

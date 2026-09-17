package com.fwdrobo.roombooking.repository;

import java.time.LocalDateTime;

import com.fwdrobo.roombooking.domain.Booking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBookingRepositoryTest {

    @Test
    void checksNewBookingsAndAdjacentWindowsWithoutChangingStoredData() {
        InMemoryBookingRepository repository = new InMemoryBookingRepository();
        LocalDateTime start = LocalDateTime.parse("2030-01-15T10:00:00");
        LocalDateTime end = start.plusHours(1);

        assertFalse(repository.existsOverlapping("room-new", start, end));
        Booking booking = repository.create("room-new", start, end);

        assertTrue(repository.existsOverlapping("room-new", start.plusMinutes(15), end.minusMinutes(15)));
        assertFalse(repository.existsOverlapping("room-new", start.minusMinutes(30), start));
        assertFalse(repository.existsOverlapping("room-new", end, end.plusMinutes(30)));
        assertFalse(repository.existsOverlapping("room-other", start, end));
        assertEquals(1L, repository.countByRoomId("room-new"));
        assertEquals(booking, repository.findByIdAndRoomId(booking.id(), "room-new").orElseThrow());
    }
}

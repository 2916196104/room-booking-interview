package com.fwdrobo.roombooking.service;

import java.time.LocalDateTime;

import com.fwdrobo.roombooking.repository.InMemoryBookingRepository;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    private final InMemoryBookingRepository bookingRepository;

    public AvailabilityService(InMemoryBookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public boolean isAvailable(String roomId, LocalDateTime start, LocalDateTime end) {
        return !bookingRepository.existsOverlapping(roomId, start, end);
    }
}

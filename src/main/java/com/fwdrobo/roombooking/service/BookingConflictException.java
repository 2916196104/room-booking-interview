package com.fwdrobo.roombooking.service;

import java.time.LocalDateTime;

public class BookingConflictException extends RuntimeException {
    //预约冲突异常
    public BookingConflictException(String roomId, LocalDateTime start, LocalDateTime end) {
        super("Room " + roomId + " is not available from " + start + " to " + end);
    }
}

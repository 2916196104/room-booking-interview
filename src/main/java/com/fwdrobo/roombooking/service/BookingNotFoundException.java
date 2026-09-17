package com.fwdrobo.roombooking.service;


public class BookingNotFoundException extends RuntimeException {
    //在指定房间内找不到该预约
    public BookingNotFoundException(String roomId, String bookingId) {
        super("Booking " + bookingId + " was not found in room " + roomId);
    }
}

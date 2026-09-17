package com.fwdrobo.roombooking.service;

import com.fwdrobo.roombooking.domain.BookingWindowResult;

public class InvalidBookingWindowException extends RuntimeException {
    //预约时间窗口无效异常
    public InvalidBookingWindowException(BookingWindowResult result) {
        super("Booking window is invalid: " + result.name());
    }
}

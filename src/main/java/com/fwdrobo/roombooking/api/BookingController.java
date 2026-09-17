package com.fwdrobo.roombooking.api;

import java.time.LocalDateTime;

import com.fwdrobo.roombooking.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/rooms/{roomId}")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }


    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @PathVariable String roomId,
            @RequestBody CreateBookingRequest request
    ) {
        BookingResponse booking = BookingResponse.from(bookingService.create(roomId, request.start(), request.end()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{bookingId}").buildAndExpand(booking.id()).toUri())
                .body(booking);
    }

    //查询单条预约
    @GetMapping("/bookings/{bookingId}")
    public BookingResponse getBooking(
            @PathVariable String roomId,
            @PathVariable String bookingId
    ) {
        return BookingResponse.from(bookingService.get(roomId, bookingId));
    }
    //查询房间在指定时间段内是否可用
    @GetMapping("/availability")
    public AvailabilityResponse getAvailability(
            @PathVariable String roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return new AvailabilityResponse(bookingService.isAvailable(roomId, start, end));
    }
}

package com.fwdrobo.roombooking.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource(textBlock = """
            overlaps first booking from right, room-202, 10:15:00, 10:45:00, false
            overlaps first booking from left,  room-202, 09:45:00, 10:15:00, false
            matches first booking exactly,     room-202, 10:00:00, 10:30:00, false
            contains first booking,            room-202, 09:45:00, 10:45:00, false
            overlaps last booking from right,  room-202, 12:15:00, 12:45:00, false
            overlaps last booking from left,   room-202, 11:45:00, 12:15:00, false
            overlaps both bookings,            room-202, 10:15:00, 12:15:00, false
            ends at first booking start,       room-202, 09:30:00, 10:00:00, true
            starts at first booking end,       room-202, 10:30:00, 11:00:00, true
            ends at last booking start,        room-202, 11:30:00, 12:00:00, true
            starts at last booking end,        room-202, 12:30:00, 13:00:00, true
            before all bookings,               room-202, 08:00:00, 08:30:00, true
            after all bookings,                room-202, 13:00:00, 13:30:00, true
            between bookings,                  room-202, 11:00:00, 11:30:00, true
            ignores another room bookings,     room-101, 10:15:00, 10:45:00, true
            """)
    void checksAvailabilityForBookingWindows(
            String scenario, String roomId, String start, String end, boolean expectedAvailable
    ) throws Exception {
        mockMvc.perform(get("/rooms/{roomId}/availability", roomId)
                        .param("start", "2030-01-15T" + start)
                        .param("end", "2030-01-15T" + end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.available").value(expectedAvailable));
    }

    @Test
    void returnsExistingBooking() throws Exception {
        mockMvc.perform(get("/rooms/room-101/bookings/booking-1011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("booking-1011"))
                .andExpect(jsonPath("$.roomId").value("room-101"))
                .andExpect(jsonPath("$.start").value("2030-01-15T09:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-15T09:30:00"));
    }

    @Test
    void returnsNotFoundForMissingRoom() throws Exception {
        mockMvc.perform(get("/rooms/room-missing/bookings/booking-1011"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/rooms/room-missing/bookings/booking-1011"));
    }
   //第二题测试
    @Test
    void returnsNotFoundForMissingBooking() throws Exception {
        mockMvc.perform(get("/rooms/room-101/bookings/booking-missing"))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$", aMapWithSize(5)),
                        jsonPath("$.status").value(404),
                        jsonPath("$.error").value("Not Found"),
                        jsonPath("$.code").value("BOOKING_NOT_FOUND"),
                        jsonPath("$.message")
                                .value("Booking booking-missing was not found in room room-101"),
                        jsonPath("$.path").value("/rooms/room-101/bookings/booking-missing"));
    }
}

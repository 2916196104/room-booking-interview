package com.fwdrobo.roombooking.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fwdrobo.roombooking.domain.Booking;
import com.fwdrobo.roombooking.domain.BookingWindowPolicy;
import com.fwdrobo.roombooking.repository.InMemoryBookingRepository;
import com.fwdrobo.roombooking.repository.InMemoryRoomRepository;
import com.fwdrobo.roombooking.service.AvailabilityService;
import com.fwdrobo.roombooking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingCreationApiTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    private InMemoryBookingRepository repository;
    private List<Booking> expectedBookings;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookingRepository();
        expectedBookings = new ArrayList<>(List.of(
                repository.findByIdAndRoomId("booking-1011", "room-101").orElseThrow(),
                repository.findByIdAndRoomId("booking-2021", "room-202").orElseThrow(),
                repository.findByIdAndRoomId("booking-2022", "room-202").orElseThrow()));
        BookingService service = new BookingService(new InMemoryRoomRepository(), repository,
                new BookingWindowPolicy(), new AvailabilityService(repository));
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource(textBlock = """
            minimum duration and other room overlap, room-101, 10:00:00, 10:30:00
            maximum duration,                        room-101, 13:00:00, 15:00:00
            one nanosecond above minimum,            room-101, 13:00:00, 13:30:00.000000001
            one nanosecond below maximum,            room-101, 13:00:00, 14:59:59.999999999
            ends at first booking start,             room-202, 09:30:00, 10:00:00
            starts at first booking end,             room-202, 10:30:00, 11:00:00
            ends at last booking start,              room-202, 11:30:00, 12:00:00
            starts at last booking end,              room-202, 12:30:00, 13:00:00
            fills gap touching both bookings,        room-202, 10:30:00, 12:00:00
            """)
    void createsBookingAndReturnsQueryableLocation(String scenario, String roomId, String start, String end)
            throws Exception {
        assertCreated(roomId, start, end, window(start, end));
    }

    @Test
    void generatesDistinctIdsAndUsesRoomFromPath() throws Exception {
        Booking first = assertCreated("room-101", "10:00:00", "10:30:00", """
                {"id":"booking-1011","roomId":"room-202",
                 "start":"2030-01-15T10:00:00","end":"2030-01-15T10:30:00"}
                """);
        Booking second = assertCreated("room-101", "10:30:00", "11:00:00", window("10:30:00", "11:00:00"));
        assertNotEquals(first.id(), second.id());
        assertNotEquals("booking-1011", first.id());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource(textBlock = """
            missing start,                  ,                  10:30:00,           MISSING_BOUNDARY
            missing end,                    10:00:00,          ,                   MISSING_BOUNDARY
            both missing,                   ,                  ,                   MISSING_BOUNDARY
            equal boundaries,               10:00:00,          10:00:00,           END_NOT_AFTER_START
            reversed boundaries,            10:00:00,          09:59:00,           END_NOT_AFTER_START
            shorter than minimum,           10:00:00,          10:29:59,           DURATION_OUT_OF_RANGE
            longer than maximum,            10:00:00,          12:00:01,           DURATION_OUT_OF_RANGE
            one nanosecond below minimum,   10:00:00,          10:29:59.999999999, DURATION_OUT_OF_RANGE
            one nanosecond above maximum,   10:00:00,          12:00:00.000000001, DURATION_OUT_OF_RANGE
            """)
    void rejectsInvalidWindowsWithoutChangingData(String scenario, String start, String end, String reason)
            throws Exception {
        assertRejected("room-202", window(start, end), HttpStatus.BAD_REQUEST, "INVALID_BOOKING_WINDOW",
                "Booking window is invalid: " + reason);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"start\":\"2030-01-15T10:00:00\"}",
            "{\"end\":\"2030-01-15T10:30:00\"}"})
    void rejectsOmittedBoundariesWithoutChangingData(String body) throws Exception {
        assertRejected("room-101", body, HttpStatus.BAD_REQUEST, "INVALID_BOOKING_WINDOW",
                "Booking window is invalid: MISSING_BOUNDARY");
    }

    @Test
    void rejectsMissingRoomWithoutChangingData() throws Exception {
        assertRejected("room-missing", window("10:00:00", "10:30:00"), HttpStatus.NOT_FOUND,
                "ROOM_NOT_FOUND", "Room room-missing was not found");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource(textBlock = """
            overlaps first booking from left,  09:45:00,           10:15:00
            overlaps first booking from right, 10:15:00,           10:45:00
            exact same window,                 10:00:00,           10:30:00
            contains existing booking,         09:45:00,           10:45:00
            overlaps last booking,             12:15:00,           12:45:00
            overlaps both bookings,            10:15:00,           12:15:00
            one nanosecond overlap at start,   09:30:00.000000001, 10:00:00.000000001
            one nanosecond overlap at end,     10:29:59.999999999, 10:59:59.999999999
            """)
    void rejectsConflictsWithoutChangingData(String scenario, String start, String end) throws Exception {
        assertConflict("room-202", start, end);
    }

    @Test
    void rejectsDuplicateAndContainedWindowsAfterCreationWithoutChangingData() throws Exception {
        assertCreated("room-101", "13:00:00", "15:00:00", window("13:00:00", "15:00:00"));
        assertConflict("room-101", "13:00:00", "15:00:00");
        assertConflict("room-101", "13:30:00", "14:00:00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{", "{\"start\":\"not-a-date\",\"end\":\"2030-01-15T10:30:00\"}"})
    void rejectsUnreadableBodyWithoutChangingData(String body) throws Exception {
        assertRejected("room-101", body, HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is malformed");
    }

    private Booking assertCreated(String roomId, String start, String end, String body) throws Exception {
        var response = mockMvc.perform(post("/rooms/{roomId}/bookings", roomId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id", not(blankOrNullString())))
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.start").value("2030-01-15T" + start))
                .andExpect(jsonPath("$.end").value("2030-01-15T" + end))
                .andExpect(header().exists("Location"))
                .andReturn().getResponse();
        Booking booking = objectMapper.readValue(response.getContentAsString(), Booking.class);
        expectedBookings.add(booking);
        assertStoredBookings();
        mockMvc.perform(get(response.getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(content().json(response.getContentAsString()));
        return booking;
    }

    private void assertConflict(String roomId, String start, String end) throws Exception {
        assertRejected(roomId, window(start, end), HttpStatus.CONFLICT, "BOOKING_CONFLICT",
                "Room " + roomId + " is not available from " + LocalDateTime.parse("2030-01-15T" + start)
                        + " to " + LocalDateTime.parse("2030-01-15T" + end));
    }

    private void assertRejected(String roomId, String body, HttpStatus expectedStatus, String code, String message)
            throws Exception {
        mockMvc.perform(post("/rooms/{roomId}/bookings", roomId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.status").value(expectedStatus.value()))
                .andExpect(jsonPath("$.error").value(expectedStatus.getReasonPhrase()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/rooms/" + roomId + "/bookings"));
        assertStoredBookings();
    }

    private void assertStoredBookings() {
        for (String roomId : List.of("room-101", "room-202", "room-missing")) {
            assertEquals(expectedBookings.stream().filter(booking -> booking.roomId().equals(roomId)).count(),
                    repository.countByRoomId(roomId), "booking count for " + roomId);
        }
        for (Booking booking : expectedBookings) {
            assertEquals(booking, repository.findByIdAndRoomId(booking.id(), booking.roomId()).orElseThrow());
        }
    }

    private String window(String start, String end) {
        return "{\"start\":" + boundary(start) + ",\"end\":" + boundary(end) + "}";
    }

    private String boundary(String time) {
        return time == null ? "null" : "\"2030-01-15T" + time + "\"";
    }
}

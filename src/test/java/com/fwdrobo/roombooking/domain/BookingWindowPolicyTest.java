package com.fwdrobo.roombooking.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingWindowPolicyTest {

    private final BookingWindowPolicy policy = new BookingWindowPolicy();

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource(textBlock = """
            missing start,                  ,                   2030-01-15T09:30:00,           MISSING_BOUNDARY
            missing end,                    2030-01-15T09:00:00, ,                             MISSING_BOUNDARY
            both missing,                   ,                   ,                             MISSING_BOUNDARY
            equal ends before duration,     2030-01-15T09:00:00, 2030-01-15T09:00:00,           END_NOT_AFTER_START
            reversed ends before duration,  2030-01-15T09:00:00, 2030-01-15T08:59:00,           END_NOT_AFTER_START
            29 minutes 59 seconds,          2030-01-15T09:00:00, 2030-01-15T09:29:59,           DURATION_OUT_OF_RANGE
            exactly 30 minutes,             2030-01-15T09:00:00, 2030-01-15T09:30:00,           VALID
            30 minutes 1 second,            2030-01-15T09:00:00, 2030-01-15T09:30:01,           VALID
            119 minutes 59 seconds,         2030-01-15T09:00:00, 2030-01-15T10:59:59,           VALID
            exactly 120 minutes,            2030-01-15T09:00:00, 2030-01-15T11:00:00,           VALID
            120 minutes 1 second,           2030-01-15T09:00:00, 2030-01-15T11:00:01,           DURATION_OUT_OF_RANGE
            ordinary 60 minutes,            2030-01-15T09:00:00, 2030-01-15T10:00:00,           VALID
            one nanosecond below minimum,   2030-01-15T09:00:00, 2030-01-15T09:29:59.999999999, DURATION_OUT_OF_RANGE
            one nanosecond above maximum,   2030-01-15T09:00:00, 2030-01-15T11:00:00.000000001, DURATION_OUT_OF_RANGE
            """)
    void evaluatesWindowInRuleOrder(
            String scenario,
            LocalDateTime start,
            LocalDateTime end,
            BookingWindowResult expected
    ) {
        assertEquals(expected, policy.evaluate(start, end), scenario);
    }
}

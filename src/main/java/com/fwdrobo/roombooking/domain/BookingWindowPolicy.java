package com.fwdrobo.roombooking.domain;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class BookingWindowPolicy {

    //判断预约起止时间是否合法
    public BookingWindowResult evaluate(LocalDateTime start, LocalDateTime end) {

        //start 或 end 缺失
        if (start == null || end == null) {
            return BookingWindowResult.MISSING_BOUNDARY;
        }

       //end 不晚于 start
        if (!end.isAfter(start)) {
            return BookingWindowResult.END_NOT_AFTER_START;
        }

       //   预约时长必须在30分钟到120分钟之间
        Duration duration = Duration.between(start, end);
        if (duration.compareTo(Duration.ofMinutes(30)) < 0
                || duration.compareTo(Duration.ofMinutes(120)) > 0) {
            return BookingWindowResult.DURATION_OUT_OF_RANGE;
        }
        return BookingWindowResult.VALID;
    }
}

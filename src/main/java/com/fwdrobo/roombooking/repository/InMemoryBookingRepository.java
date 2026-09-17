package com.fwdrobo.roombooking.repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.fwdrobo.roombooking.domain.Booking;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBookingRepository {

    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(3001);

    //负责保存、查询预约
    public InMemoryBookingRepository() {
        saveSeed(new Booking(
                "booking-1011",
                "room-101",
                LocalDateTime.parse("2030-01-15T09:00:00"),
                LocalDateTime.parse("2030-01-15T09:30:00")));
        saveSeed(new Booking(
                "booking-2021",
                "room-202",
                LocalDateTime.parse("2030-01-15T10:00:00"),
                LocalDateTime.parse("2030-01-15T10:30:00")));
        saveSeed(new Booking(
                "booking-2022",
                "room-202",
                LocalDateTime.parse("2030-01-15T12:00:00"),
                LocalDateTime.parse("2030-01-15T12:30:00")));
    }

    //查询指定预约，并确认它属于指定房间
    public synchronized Optional<Booking> findByIdAndRoomId(String bookingId, String roomId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null || !booking.roomId().equals(roomId)) {
            return Optional.empty();
        }
        return Optional.of(booking);
    }
    // ponytail: 内存查询最坏仍扫描全部预约；大数据量时改用数据库 EXISTS 和合适的索引。
    public synchronized boolean existsOverlapping(String roomId, LocalDateTime start, LocalDateTime end) {
        return bookings.values().stream()
                .anyMatch(booking -> booking.roomId().equals(roomId)
                        && start.isBefore(booking.end())
                        && booking.start().isBefore(end));
    }

    //生成预约编号并保存预约
    public synchronized Booking create(String roomId, LocalDateTime start, LocalDateTime end) {
        String bookingId = "booking-" + nextId.getAndIncrement();
        Booking booking = new Booking(bookingId, roomId, start, end);
        bookings.put(bookingId, booking);
        return booking;
    }

    //统计某个房间的预约数量
    public synchronized long countByRoomId(String roomId) {
        return bookings.values().stream()
                .filter(booking -> booking.roomId().equals(roomId))
                .count();
    }

    //写入启动时的预置数据
    private void saveSeed(Booking booking) {
        bookings.put(booking.id(), booking);
    }
}

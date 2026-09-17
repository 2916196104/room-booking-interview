package com.fwdrobo.roombooking.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fwdrobo.roombooking.domain.Room;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRoomRepository {

    private final Map<String, Room> rooms = new LinkedHashMap<>();
    //负责保存、查询房间
    public InMemoryRoomRepository() {
        rooms.put("room-101", new Room("room-101", "Quiet Room"));
        rooms.put("room-202", new Room("room-202", "Focus Room"));
    }
   //查询不到
    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }
}

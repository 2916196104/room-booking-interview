package com.fwdrobo.roombooking.service;

public class RoomNotFoundException extends RuntimeException {
    //房间不存在异常
    public RoomNotFoundException(String roomId) {
        super("Room " + roomId + " was not found");
    }
}

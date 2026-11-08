package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.OfficeRoomInput;
import com.gocent.pdjh.entity.OfficeRoom;
import java.util.List;

public interface OfficeRoomService {
    List<OfficeRoom> getAllRooms();
    List<OfficeRoom> getRoomsByDeptId(String deptId);
    boolean saveRoom(OfficeRoomInput input);
    boolean removeRoom(String id);
}

package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.OfficeRoomBrief;

import java.util.List;

public interface ScreenRoomMappingService {
    boolean relevancyScreenAndRooms(String screenId, List<String> officeRoomIds);
    List<OfficeRoomBrief> getAssociatedRooms(String screenId);
    String buildRoomInfoString(String screenId);
}

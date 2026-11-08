package com.gocent.pdjh.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScreenRoomMappingInput {
    private String screenId;
    private List<String> officeRoomIds;
}

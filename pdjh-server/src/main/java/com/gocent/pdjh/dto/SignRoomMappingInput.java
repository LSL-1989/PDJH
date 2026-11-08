package com.gocent.pdjh.dto;

import lombok.Data;

import java.util.List;

@Data
public class SignRoomMappingInput {
    private String signDeviceId;
    private List<String> officeRoomIds;
}

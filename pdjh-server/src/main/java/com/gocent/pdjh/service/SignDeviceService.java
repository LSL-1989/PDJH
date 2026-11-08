package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.SignDeviceConfigOutput;
import com.gocent.pdjh.dto.SignDeviceInput;
import com.gocent.pdjh.dto.SignDeviceOutput;

import java.util.List;

public interface SignDeviceService {
    List<SignDeviceOutput> listAll(String name);
    SignDeviceConfigOutput getDeviceConfig(String deviceCode);
    SignDeviceOutput save(SignDeviceInput input);
    boolean remove(String id);
    boolean relevancySignDeviceAndRooms(String signDeviceId, List<String> officeRoomIds);
}

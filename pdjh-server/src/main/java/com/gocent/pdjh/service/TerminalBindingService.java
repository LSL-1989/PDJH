package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.OfficeRoomBrief;
import com.gocent.pdjh.dto.SignDeviceConfigOutput;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.SignDevice;

import java.util.List;

public interface TerminalBindingService {

    SignDevice resolveSignDevice(String deviceCode);

    List<String> resolveSignDeviceRoomIds(String deviceCode);

    List<String> resolveScreenRoomIds(String screenId);

    SignDeviceConfigOutput buildSignDeviceConfig(SignDevice device, List<String> roomIds);

    List<OfficeRoomBrief> toRoomBriefs(List<String> roomIds);

    void assertRoomAllowed(String roomId, List<String> allowedRoomIds);

    void assertScreenRoomBinding(String screenId);

    CallList matchPendingSignIn(String patientCode, List<String> allowedRoomIds);

    String buildWrongAreaMessage(CallList callList);
}

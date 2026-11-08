package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.VoiceDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoiceDeviceRepository extends JpaRepository<VoiceDevice, String> {
    Optional<VoiceDevice> findByDeviceCode(String deviceCode);
    List<VoiceDevice> findByState(Integer state);
    List<VoiceDevice> findByScreenId(String screenId);
    boolean existsByDeviceCode(String deviceCode);
}

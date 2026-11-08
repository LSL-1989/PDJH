package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.VoiceScopeMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoiceScopeMappingRepository extends JpaRepository<VoiceScopeMapping, Integer> {
    List<VoiceScopeMapping> findByVoiceDeviceId(String voiceDeviceId);
    List<VoiceScopeMapping> findByScopeTypeAndScopeId(String scopeType, String scopeId);
    void deleteByVoiceDeviceId(String voiceDeviceId);
    void deleteByScopeTypeAndScopeId(String scopeType, String scopeId);
}

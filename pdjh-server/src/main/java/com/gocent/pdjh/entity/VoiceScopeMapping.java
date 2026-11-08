package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 独立音响绑定范围。
 * scopeType=SCREEN → scopeId=doc_screen.id（候诊大屏端绑定音响，多对多，推荐）
 * scopeType=ROOM → scopeId=office_room.id（旧）
 * scopeType=DEPT → scopeId=department.id（旧）
 */
@Data
@Entity
@Table(name = "voice_scope_mapping", indexes = {
        @Index(name = "idx_voice_scope_device", columnList = "voice_device_id"),
        @Index(name = "idx_voice_scope_type_id", columnList = "scope_type,scope_id")
})
public class VoiceScopeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "voice_device_id", length = 36, nullable = false)
    private String voiceDeviceId;

    /** SCREEN | ROOM | DEPT */
    @Column(name = "scope_type", length = 16, nullable = false)
    private String scopeType;

    @Column(name = "scope_id", length = 64, nullable = false)
    private String scopeId;
}

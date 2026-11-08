package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sign_room_mapping")
public class SignRoomMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sign_device_id", length = 36)
    private String signDeviceId;

    @Column(name = "office_room_id", length = 64)
    private String officeRoomId;
}

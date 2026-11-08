package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "screen_room_mapping")
public class ScreenRoomMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "screen_id", length = 36)
    private String screenId;

    @Column(name = "office_room_id", length = 64)
    private String officeRoomId;
}

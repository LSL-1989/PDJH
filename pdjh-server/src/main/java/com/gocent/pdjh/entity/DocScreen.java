package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "doc_screen")
public class DocScreen {
    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 100)
    private String name;

    private Integer state;

    @Column(name = "create_date")
    private java.util.Date createDate;

    @Column(name = "call_code", length = 50)
    private String callCode;

    @Column(name = "call_num")
    private Integer callNum;

    @Column(name = "call_content", length = 500)
    private String callContent;

    @Column(name = "max_num")
    private Integer maxNum;

    @Column(length = 1000)
    private String bulletin;

    @Column(name = "scroll_time")
    private Integer scrollTime;

    @Column(length = 200)
    private String subtitle;

    private Integer rate;

    @Column(name = "data_mode")
    private Integer dataMode;

    @Column(name = "is_num", length = 10)
    private String isNum;

    @Column(name = "is_audio", length = 10)
    private String isAudio;

    /**
     * 播报通道：VOICE=仅独立音响；SCREEN=仅候诊大屏 TTS；BOTH=同时。
     * 音响故障时切到 SCREEN。默认 VOICE。
     */
    @Column(name = "voice_channel", length = 16)
    private String voiceChannel;

    @Column(name = "device_code", length = 100, unique = true)
    private String deviceCode;

    /** 终端类型：SCREEN 候诊大屏 / CLINIC 门边屏 */
    @Column(name = "terminal_type", length = 20)
    private String terminalType;

    /** 候诊区模板路由，如 /default、/pharmacy-window、/lab-window、/GlobalHub */
    @Column(name = "template_route", length = 50)
    private String templateRoute;

    @Column(name = "start_time")
    private java.util.Date startTime;

    @Column(name = "end_time")
    private java.util.Date endTime;

    /** 终端最近一次 Register/Resolve 时间，用于判断连接状态 */
    @Column(name = "last_seen_at")
    private java.util.Date lastSeenAt;
}

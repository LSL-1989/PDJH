package com.gocent.pdjh.dto;

import lombok.Data;

/**
 * 医生站改队列状态入参（兼容旧字段 calllistid/state）。
 * 可选 officeRoomId/doctorId：呼叫接诊(state=1)时绑定诊室与医生，供门边屏按诊室刷新。
 */
@Data
public class DocCallMsgInputUp {
    /** 新状态：1就诊中 4完成 6过号 */
    private Integer state;
    /** 排队记录 ID */
    private Integer calllistid;
    /** 可选：绑定诊室（与门边屏 office_room 对齐） */
    private String officeRoomId;
    /** 可选：绑定医生（doctor.id） */
    private String doctorId;
}

package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.VoiceDeviceInput;
import com.gocent.pdjh.dto.VoiceDeviceOutput;
import com.gocent.pdjh.dto.VoiceScopeMappingInput;
import com.gocent.pdjh.entity.VoiceDevice;

import java.util.List;
import java.util.Map;

public interface VoiceDeviceService {
    List<VoiceDeviceOutput> listAll();
    VoiceDevice save(VoiceDeviceInput input);
    boolean remove(String id);

    /** @deprecated 旧诊室/诊区绑定；新版由候诊大屏端绑定音响 */
    boolean bindScopes(VoiceScopeMappingInput input);

    /**
     * 候诊大屏端绑定独立音响（多对多）：一台大屏可绑多台音响，一台音响也可被多块大屏共用。
     */
    void bindVoiceDevicesToScreen(String screenId, List<String> voiceDeviceIds);

    /** 查询某候诊大屏已绑定的音响设备 ID */
    List<String> listVoiceDeviceIdsForScreen(String screenId);

    /** 绑定摘要文案（后台列表） */
    String summarizeVoiceBinding(String screenId);

    VoiceDeviceOutput getByDeviceCode(String deviceCode);

    /**
     * 按诊室关联的候诊大屏解析独立音响并推送。
     * 播报文案使用各大屏「语音播报」CallContent 模板：{0}=号序 {1}=姓名 {2}=诊室。
     * @return 实际推送到的音响数量（0 表示无可用音响）
     */
    int broadcastCall(String officeRoomId, String showNo, String patientName, String office, Map<String, Object> extra);

    /**
     * 按诊室 / 呼叫码解析候诊大屏 CallContent，生成叫号播报文案。
     */
    String formatCallText(String officeRoomId, String callCode, String showNo, String patientName, String office);

    /**
     * 解析叫号涉及候诊大屏的播报通道：VOICE | SCREEN | BOTH。
     */
    String resolveVoiceChannel(String officeRoomId, String callCode);

    /** 解析应负责大屏 TTS 的候诊大屏 ID（无则 null） */
    String resolveSpeakScreenId(String officeRoomId, String callCode);

    /**
     * 解析关联候诊大屏的呼叫次数、语速（keys: callNum, rate）。
     * 无关联大屏时 callNum=1、rate=0。
     */
    Map<String, Object> resolveSpeakOpts(String officeRoomId, String callCode);
}

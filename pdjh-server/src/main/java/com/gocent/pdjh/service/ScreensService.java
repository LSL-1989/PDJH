package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.ScreensInput;
import com.gocent.pdjh.dto.ScreensOutput;

import java.util.List;

public interface ScreensService {
    
    // 获取屏幕列表
    List<ScreensOutput> getScreens(ScreensInput input);

    // 获取单个屏幕
    ScreensOutput getScreensSingle(String id);

    // 添加屏幕
    boolean addScreen(ScreensInput input);

    // 更新屏幕
    boolean updateScreen(ScreensInput input);

    // 删除屏幕
    boolean delScreen(String id);

    // 根据设备识别码获取屏幕分配数据
    ScreensOutput getScreenByDeviceCode(String deviceCode);
}

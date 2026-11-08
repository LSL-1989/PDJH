package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 兼容旧版签到端 System/GetDataTime
 */
@RestController
@RequestMapping("/System")
public class SystemController {

    @GetMapping("/GetDataTime")
    public ResultData<Map<String, String>> getDataTime() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return ResultData.success(Map.of("DataTime", now), "获取成功");
    }
}

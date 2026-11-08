package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.SystemActionLog;
import com.gocent.pdjh.repository.SystemActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/SystemActionLog")
@RequiredArgsConstructor
public class SystemActionLogController {

    private final SystemActionLogRepository systemActionLogRepository;

    @PostMapping("/Query")
    public ResultData<Map<String, Object>> queryLogs(@RequestBody Map<String, Object> input) {
        String logType = (String) input.get("logType");
        if (logType != null && logType.isEmpty()) logType = null;
        
        String keyword = (String) input.get("keyword");
        if (keyword != null && keyword.isEmpty()) keyword = null;

        Date startDate = null;
        Date endDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            if (input.get("startDate") != null && !input.get("startDate").toString().isEmpty()) {
                startDate = sdf.parse(input.get("startDate").toString() + " 00:00:00");
            }
            if (input.get("endDate") != null && !input.get("endDate").toString().isEmpty()) {
                endDate = sdf.parse(input.get("endDate").toString() + " 23:59:59");
            }
        } catch (ParseException e) {
            return ResultData.fail("日期格式错误，请使用 yyyy-MM-dd 格式");
        }

        int pageIdx = input.containsKey("pageIndex") ? (int) input.get("pageIndex") - 1 : 0;
        int pageSize = input.containsKey("pageSize") ? (int) input.get("pageSize") : 20;

        Pageable pageable = PageRequest.of(pageIdx, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<SystemActionLog> pageResult = systemActionLogRepository.searchLogs(logType, keyword, startDate, endDate, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("TotalCount", pageResult.getTotalElements());
        result.put("data", pageResult.getContent());

        return ResultData.success(result, "查询业务日志成功");
    }
}

package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.CallListScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/CallList")
@RequiredArgsConstructor
public class CallListController {

    private final CallListScreenService callListScreenService;

    @PostMapping("/GetCallListInput")
    public ResultData<List<Map<String, Object>>> getCallListInput(@RequestBody Map<String, Object> body) {
        String screenId = str(body, "ID", "id", "ScreensID", "screensID");
        if (screenId == null) {
            return ResultData.fail(400, "大屏ID不能为空");
        }
        return ResultData.success(callListScreenService.getCallListInput(screenId), "获取成功");
    }

    /**
     * 大屏分页队列。includePreparing=true 时 state=0 含药房配药中，供 /pharmacy-window；
     * 门诊大屏不传该字段。与 pharmacyDeskQueue 的区别：按屏幕绑定范围过滤，返回大屏字段。
     */
    @PostMapping("/GetCallListInputPage")
    public ResultData<Map<String, Object>> getCallListInputPage(@RequestBody Map<String, Object> body) {
        String screenId = str(body, "ScreensID", "screensID", "ID", "id");
        String code = str(body, "Code", "code");
        Integer state = intVal(body.get("state"));
        Integer index = intVal(body.get("index"));
        Integer page = intVal(body.get("page"));
        if (screenId == null) {
            return ResultData.fail(400, "ScreensID 不能为空");
        }
        return ResultData.success(
                callListScreenService.getCallListInputPage(screenId, code, state, index, page, boolVal(body.get("includePreparing"))),
                "获取成功");
    }

    private static String str(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.get(key) != null) {
                return body.get(key).toString();
            }
        }
        return null;
    }

    private static Integer intVal(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean boolVal(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v == null) return false;
        String s = v.toString();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }
}

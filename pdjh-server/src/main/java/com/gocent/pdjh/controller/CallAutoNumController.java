package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.CallAutoNumSyncResult;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.CallAutoNum;
import com.gocent.pdjh.service.CallAutoNumService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 编号规则管理（call_auto_num）接口：AutoNumManages 页 */
@RestController
@RequestMapping("/CallAutoNum")
@RequiredArgsConstructor
public class CallAutoNumController {

    private final CallAutoNumService callAutoNumService;

    @PostMapping("/GetAutoNum")
    public ResultData<List<CallAutoNum>> getAutoNum() {
        return ResultData.success(callAutoNumService.listAll(), "获取成功");
    }

    @PostMapping("/CreateAutoNum")
    public ResultData<Boolean> create(@RequestBody Map<String, Object> body) {
        return callAutoNumService.create(body)
                ? ResultData.success(true, "新增成功")
                : ResultData.fail(500, "新增失败");
    }

    @PostMapping("/EditorAutoNum")
    public ResultData<Boolean> editor(@RequestBody Map<String, Object> body) {
        return callAutoNumService.update(body)
                ? ResultData.success(true, "修改成功")
                : ResultData.fail(500, "修改失败");
    }

    @PostMapping("/RemoveAutoNum")
    public ResultData<Boolean> remove(@RequestParam("GID") String gid) {
        return callAutoNumService.remove(gid)
                ? ResultData.success(true, "删除成功")
                : ResultData.fail(500, "删除失败");
    }

    /**
     * 按当前用户角色已授权的主任工作台权限，同步当日各科室三类发号序号到 call_auto_num。
     * body: { token, autoDate? }；已存在记录则更新，数据写入库后同权限用户共享。
     */
    @PostMapping("/SyncAutoNum")
    public ResultData<CallAutoNumSyncResult> syncAutoNum(@RequestBody Map<String, Object> body) {
        try {
            String token = body.get("token") != null ? body.get("token").toString() : null;
            String autoDate = body.get("autoDate") != null ? body.get("autoDate").toString() : null;
            CallAutoNumSyncResult result = callAutoNumService.syncFromCallList(token, autoDate);
            if (result.getBusinessTypes().isEmpty()) {
                return ResultData.fail(400, "当前角色未授权任何主任工作台，无可同步的业务类型");
            }
            String msg = String.format(
                    "同步完成：%d 个科室（新增 %d，更新 %d），业务类型 %s",
                    result.getSyncedCount(),
                    result.getCreatedCount(),
                    result.getUpdatedCount(),
                    result.getBusinessTypes());
            return ResultData.success(result, msg);
        } catch (IllegalArgumentException e) {
            return ResultData.fail(401, e.getMessage());
        } catch (Exception e) {
            return ResultData.fail(500, "同步失败：" + e.getMessage());
        }
    }
}

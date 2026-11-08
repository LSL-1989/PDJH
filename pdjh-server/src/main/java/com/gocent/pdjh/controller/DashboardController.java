package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/getOverviewMetrics")
    public ResultData<Map<String, Object>> getOverviewMetrics() {
        return ResultData.success(dashboardService.getOverviewMetrics(), "获取成功");
    }

    /** 后台首页待处理消息栏 */
    @GetMapping("/pending-summary")
    public ResultData<Map<String, Object>> pendingSummary() {
        return ResultData.success(dashboardService.getPendingSummary(), "获取成功");
    }

    @PostMapping("/pending-resolve")
    public ResultData<Boolean> pendingResolve(@RequestBody Map<String, Object> body) {
        Object id = body.get("id");
        if (id == null) {
            return ResultData.fail(400, "id 不能为空");
        }
        dashboardService.resolvePending(Long.parseLong(id.toString()));
        return ResultData.success(true, "已标记处理");
    }

    @GetMapping("/getOvertimeAlerts")
    public ResultData<List<Map<String, Object>>> getOvertimeAlerts(@RequestParam(defaultValue = "45") int thresholdMinutes) {
        return ResultData.success(dashboardService.getOvertimeAlerts(thresholdMinutes), "获取成功");
    }

    @GetMapping("/getPeakAnalysis")
    public ResultData<List<Map<String, Object>>> getPeakAnalysis(@RequestParam(required = false) String date) {
        return ResultData.success(dashboardService.getPeakAnalysis(date), "获取成功");
    }

    @GetMapping("/heatmap-matrix")
    public ResultData<List<Map<String, Object>>> getHeatmapMatrix() {
        return ResultData.success(dashboardService.getHeatmapMatrix(), "获取成功");
    }

    /** 门诊主任台：医生出诊实时看板（维护医生档案 + 排队实况） */
    @GetMapping("/outpatient/doctor-duty-board")
    public ResultData<List<Map<String, Object>>> outpatientDoctorDutyBoard() {
        return ResultData.success(dashboardService.getOutpatientDoctorDutyBoard(), "获取成功");
    }

    /** 门诊主任台：科室过号率排行 */
    @GetMapping("/outpatient/skip-rate-ranking")
    public ResultData<List<Map<String, Object>>> outpatientSkipRateRanking() {
        return ResultData.success(dashboardService.getOutpatientSkipRateRanking(), "获取成功");
    }

    /** 门诊主任台：今日号源配额总览 */
    @GetMapping("/outpatient/quota-overview")
    public ResultData<List<Map<String, Object>>> outpatientQuotaOverview() {
        return ResultData.success(dashboardService.getOutpatientQuotaOverview(), "获取成功");
    }

    @GetMapping("/legacy/departments")
    public ResultData<List<Map<String, Object>>> legacyDepartments() {
        return ResultData.success(dashboardService.legacyDepartmentWaitCards(), "获取成功");
    }

    @PostMapping("/legacy/department-queue-time")
    public ResultData<List<Map<String, Object>>> legacyDepartmentQueueTime(@RequestBody Map<String, Object> body) {
        return ResultData.success(dashboardService.legacyDepartmentQueueTime(body), "获取成功");
    }

    @PostMapping("/legacy/doctor-call-count")
    public ResultData<Map<String, Object>> legacyDoctorCallCount(@RequestBody Map<String, Object> body) {
        return ResultData.success(dashboardService.legacyDoctorCallCount(body), "获取成功");
    }

    @GetMapping("/statistics/summary")
    public ResultData<Map<String, Object>> statisticsSummary(@RequestParam(required = false) String date) {
        return ResultData.success(dashboardService.getStatisticsSummary(date), "获取成功");
    }

    @GetMapping("/statistics/hourly")
    public ResultData<List<Map<String, Object>>> statisticsHourly(@RequestParam(required = false) String date) {
        return ResultData.success(dashboardService.getStatisticsHourly(date), "获取成功");
    }

    @GetMapping("/statistics/dept-radar")
    public ResultData<List<Map<String, Object>>> statisticsDeptRadar(@RequestParam(required = false) String date) {
        return ResultData.success(dashboardService.getStatisticsDeptRadar(date), "获取成功");
    }
}

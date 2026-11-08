package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.ImplementTask;
import com.gocent.pdjh.repository.ImplementTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/implement")
public class ImplementTaskController {

    @Autowired
    private ImplementTaskRepository implementTaskRepository;

    /**
     * 获取全部实施节点打卡快照
     */
    @GetMapping("/getAllTasks")
    public ResultData<?> getAllTasks() {
        List<ImplementTask> tasks = implementTaskRepository.findAll();
        return ResultData.success(tasks);
    }

    /**
     * 获取单一任务详情
     */
    @GetMapping("/getTaskByCode")
    public ResultData<?> getTaskByCode(@RequestParam String stepCode) {
        ImplementTask task = implementTaskRepository.findByStepCode(stepCode);
        return ResultData.success(task);
    }

    /**
     * 初始化或获取并开启计时的前置节点 (实施人员点击"去实施 / 开启打卡")
     */
    @PostMapping("/startTask")
    public ResultData<?> startTask(@RequestBody ImplementTask req) {
        ImplementTask task = implementTaskRepository.findByStepCode(req.getStepCode());
        if (task == null) {
            task = new ImplementTask();
            task.setStepCode(req.getStepCode());
            task.setPhaseCode(req.getPhaseCode());
            task.setStepName(req.getStepName());
            task.setCreateTime(new Date());
        }
        
        // 如果是未开始或刚创，设定状态为 1进行中
        if (task.getStatus() == null || task.getStatus() == 0) {
            task.setStatus(1);
            task.setStartTime(new Date());
            task.setImplementer(req.getImplementer());
        }
        
        implementTaskRepository.save(task);
        return ResultData.success(task, "该任务项已挂载打卡挂钟开启计费追踪");
    }

    /**
     * 提交闭环单据并结算用时 (实施人员点击"我已确认无误验收提交")
     */
    @PostMapping("/finishTask")
    public ResultData<?> finishTask(@RequestBody ImplementTask req) {
        ImplementTask task = implementTaskRepository.findByStepCode(req.getStepCode());
        if (task == null) {
            return ResultData.fail("未找到正在计时的此实施项，请先点开始任务！");
        }
        
        // 覆盖笔记备注与状态
        task.setRemark(req.getRemark());
        task.setStatus(2); // 变为已验收态
        task.setFinishTime(new Date());
        
        // 核算耗时 (如果中途发生断联，则由用户前端传回人工评估的时长或走服务器兜底均可，此处优先使用传来的时长)
        if (req.getTimeSpentMinutes() != null && req.getTimeSpentMinutes() > 0) {
            task.setTimeSpentMinutes(req.getTimeSpentMinutes());
        } else {
            if (task.getStartTime() != null) {
                long diff = task.getFinishTime().getTime() - task.getStartTime().getTime();
                int minutes = (int) (diff / (1000 * 60));
                task.setTimeSpentMinutes(minutes);
            } else {
                task.setTimeSpentMinutes(0); // 安全兜底
            }
        }
        
        implementTaskRepository.save(task);
        return ResultData.success(task, "此项实施参数已成功验收入档，恭喜打卡完毕！");
    }

    /**
     * 后台反悔工具：重新激活已关上的任务 (实施翻车时)
     */
    @PostMapping("/revertTask")
    public ResultData<?> revertTask(@RequestBody ImplementTask req) {
        ImplementTask task = implementTaskRepository.findByStepCode(req.getStepCode());
        if (task != null && task.getStatus() != null && task.getStatus() == 2) {
            task.setStatus(1); // 打回重做态
            task.setStartTime(new Date());
            task.setFinishTime(null);
            implementTaskRepository.save(task);
            return ResultData.success(task, "翻车挽救成功，打回重做态");
        }
        return ResultData.success(task);
    }

}

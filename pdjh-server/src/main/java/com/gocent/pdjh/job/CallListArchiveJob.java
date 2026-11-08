package com.gocent.pdjh.job;

import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.CallListHistory;
import com.gocent.pdjh.repository.CallListHistoryRepository;
import com.gocent.pdjh.repository.CallListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallListArchiveJob {

    private final CallListRepository callListRepository;
    private final CallListHistoryRepository callListHistoryRepository;

    /**
     * 每天凌晨 2:00 提取已完结或已过号（State = 2, 3）且属于昨晚之前产生的就诊记录，进行归档沉淀入冷数据库。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void archiveCompletedCallLists() {
        log.info("[归档引擎] CallList 冷热分离批处理作业启动...");
        try {
            // 获取完结状态（2: 已就诊结束, 3: 已过号放弃）且非今日生命周期的历史流水
            List<CallList> recordsToArchive = callListRepository.findHistoryListToArchive(Arrays.asList(2, 3));
            
            if (recordsToArchive.isEmpty()) {
                log.info("[归档引擎] 无需要归档的冗余历史数据，作业结束。");
                return;
            }

            log.info("[归档引擎] 发现需要归档的历史记录条数: {}", recordsToArchive.size());

            // 对象属性深拷贝转换入冷库实体
            List<CallListHistory> historyRecords = recordsToArchive.stream().map(record -> {
                CallListHistory history = new CallListHistory();
                BeanUtils.copyProperties(record, history, "id"); // 略过 ID 以允许历史表独立自增管理
                return history;
            }).collect(Collectors.toList());

            // 批量持久化进入历史归档副表
            callListHistoryRepository.saveAll(historyRecords);
            log.info("[归档引擎] 成功持久化 {} 条记录至历史归档表 (call_list_history)。", historyRecords.size());

            // 获取原表主键集合
            List<Integer> idsToDelete = recordsToArchive.stream()
                    .map(CallList::getId)
                    .collect(Collectors.toList());

            // 反向执行彻底清理原在线表脏数据
            callListRepository.deleteArchivedRecords(idsToDelete);
            log.info("[归档引擎] 在线业务主表 (call_list) 清理完毕，彻底释放排队查询性能！");

        } catch (Exception e) {
            log.error("[归档引擎] 遇到不可预期异常，归档操作回滚！", e);
        }
    }
}

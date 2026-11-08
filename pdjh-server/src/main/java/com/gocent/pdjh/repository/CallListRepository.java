package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.CallList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CallListRepository extends JpaRepository<CallList, Integer>, JpaSpecificationExecutor<CallList> {

    // 假设查询条件有 deptID 或医生对应诊室
    List<CallList> findByDeptIDAndStateAndIsForzen(String deptId, Integer state, Integer isForzen);

    // 查询当天的排队列表 (增加预约四维优先级排序以及分诊隔离池)
    @Query("SELECT c FROM CallList c WHERE c.deptID = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING' OR c.triageState = 'REVISIT') " +
           "ORDER BY c.isAppointment DESC, c.appointmentStartTime ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findTodayCallListByDeptAndState(String deptId, Integer state);
    
    // -- 初诊专用查询 --
    @Query("SELECT c FROM CallList c WHERE c.deptID = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING') " +
           "ORDER BY c.isAppointment DESC, c.appointmentStartTime ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findTodayFirstVisitList(String deptId, Integer state);

    // -- 回诊专用查询 --
    @Query("SELECT c FROM CallList c WHERE c.deptID = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND c.triageState = 'REVISIT' " +
           "ORDER BY c.isAppointment DESC, c.appointmentStartTime ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findTodayReturnVisitList(String deptId, Integer state);

    // -- 药房可叫号队列：仅 WAITING（配药完成）；PREPARING 不可叫 --
    @Query("SELECT c FROM CallList c WHERE c.deptID = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING') " +
           "ORDER BY c.xh ASC, c.callTime ASC")
    List<CallList> findPharmacyQueueList(String deptId, Integer state);

    // -- 药房可叫号队列 (急诊/优抚特权抢占流) --
    @Query("SELECT c FROM CallList c WHERE c.deptID = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING') " +
           "ORDER BY c.xhlb ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findPharmacyQueueListUrgentFirst(String deptId, Integer state);
    
    // 按患者编号和状态查询最新的排队记录
    CallList findFirstByPatientCodeAndStateOrderByIdDesc(String patientCode, Integer state);

    // [Triage Engine] 按患者和分诊微服务状态抓取最新单据 (如 UNARRIVED)
    CallList findFirstByPatientCodeAndTriageStateOrderByIdDesc(String patientCode, String triageState);

    List<CallList> findByPatientCodeAndState(String patientCode, Integer state);

    List<CallList> findByPatientCodeAndTriageStateAndOfficeRoomIdInOrderByIdDesc(
            String patientCode, String triageState, List<String> officeRoomIds);

    /** 检验签到幂等：当日该患者在本机绑定窗口内的单据（最新在前） */
    @Query("SELECT c FROM CallList c WHERE c.patientCode = ?1 AND c.officeRoomId IN ?2 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE ORDER BY c.id DESC")
    List<CallList> findTodayByPatientCodeAndOfficeRoomIdIn(String patientCode, List<String> officeRoomIds);

    @Query("SELECT c FROM CallList c WHERE c.officeRoomId = ?1 AND c.state = ?2 AND c.isForzen = 0 " +
           "AND FUNCTION('DATE', c.callDate) = CURRENT_DATE ORDER BY c.xh ASC, c.callTime ASC")
    List<CallList> findTodayCallListByOfficeRoomAndState(String officeRoomId, Integer state);

    /** 医生站诊室粒度：等候队列（含初/复诊，与科室口径 triage 一致） */
    @Query("SELECT c FROM CallList c WHERE c.officeRoomId = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING' OR c.triageState = 'REVISIT') " +
           "ORDER BY c.isAppointment DESC, c.appointmentStartTime ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findTodayCallListByOfficeRoomAndStateForDoctor(String officeRoomId, Integer state);

    @Query("SELECT c FROM CallList c WHERE c.officeRoomId = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING') " +
           "ORDER BY c.isAppointment DESC, c.appointmentStartTime ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findTodayFirstVisitListByOfficeRoom(String officeRoomId, Integer state);

    @Query("SELECT c FROM CallList c WHERE c.officeRoomId = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND c.triageState = 'REVISIT' " +
           "ORDER BY c.isAppointment DESC, c.appointmentStartTime ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findTodayReturnVisitListByOfficeRoom(String officeRoomId, Integer state);

    @Query("SELECT c FROM CallList c WHERE c.officeRoomId = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING') " +
           "ORDER BY c.xh ASC, c.callTime ASC")
    List<CallList> findPharmacyQueueListByOfficeRoom(String officeRoomId, Integer state);

    @Query("SELECT c FROM CallList c WHERE c.officeRoomId = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND (c.triageState IS NULL OR c.triageState = 'WAITING') " +
           "ORDER BY c.xhlb ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findPharmacyQueueListUrgentFirstByOfficeRoom(String officeRoomId, Integer state);

    /** 药房辅助台：某窗口当日配药中/可取药列表（含 PREPARING） */
    @Query("SELECT c FROM CallList c WHERE c.officeRoomId = ?1 AND c.state = ?2 AND c.isForzen = 0 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE " +
           "AND c.triageState IN ('PREPARING', 'WAITING') " +
           "ORDER BY c.triageState ASC, c.xh ASC, c.callTime ASC")
    List<CallList> findPharmacyDeskQueueByOfficeRoom(String officeRoomId, Integer state);

    @Query("SELECT c FROM CallList c WHERE c.officeRoomId IN ?1 AND c.state = ?2 AND c.isForzen = 0 " +
           "AND FUNCTION('DATE', c.callDate) = CURRENT_DATE ORDER BY c.xh ASC, c.callTime ASC")
    List<CallList> findTodayCallListByOfficeRoomsAndState(List<String> officeRoomIds, Integer state);

    // 全局：查询当天各个科室还在等待（state=0 且已报到或回流）的总人数
    @Query(value = "SELECT dept_id as deptId, COUNT(id) as waitCount FROM call_list WHERE state = 0 AND is_forzen = 0 AND DATE(call_date) = CURDATE() AND (triage_state IS NULL OR triage_state = 'WAITING' OR triage_state = 'REVISIT') GROUP BY dept_id", nativeQuery = true)
    List<com.gocent.pdjh.dto.DeptWaitSummary> findGlobalWaitingSummary();

    // 提取当日病人的任意最新记录（用于自动推断复诊扫码机制）
    @Query(value = "SELECT * FROM call_list WHERE patient_code = ?1 AND DATE(call_date) = CURDATE() ORDER BY id DESC LIMIT 1", nativeQuery = true)
    CallList findTodayLatestByPatientCode(String patientCode);

    // [冷热分离] 获取需要在深夜抽离的历史生命周期终结单据（不包含本日以保证在线大屏连贯性）
    @Query("SELECT c FROM CallList c WHERE c.state IN ?1 AND FUNCTION('DATE', c.callDate) < CURRENT_DATE")
    List<CallList> findHistoryListToArchive(List<Integer> states);

    // [冷热分离] 物理转移后蒸发源表数据
    @Modifying
    @Transactional
    @Query("DELETE FROM CallList c WHERE c.id IN ?1")
    void deleteArchivedRecords(List<Integer> ids);

    @Query("SELECT COALESCE(MAX(c.xh), 0) FROM CallList c WHERE c.deptID = ?1 AND FUNCTION('DATE', c.callDate) = CURRENT_DATE")
    Integer findMaxXhTodayByDept(String deptId);

    /**
     * 按科室、日期汇总三类队列当日已发最大序号（xhlb A/B/C；空类别归入 C）
     */
    @Query(value = """
            SELECT dept_id AS deptId,
                   COALESCE(MAX(CASE WHEN xhlb = 'A' THEN xh END), 0) AS maxA,
                   COALESCE(MAX(CASE WHEN xhlb = 'B' THEN xh END), 0) AS maxB,
                   COALESCE(MAX(CASE WHEN xhlb = 'C' OR xhlb IS NULL OR xhlb = '' THEN xh END), 0) AS maxC
            FROM call_list
            WHERE DATE(call_date) = :syncDate AND dept_id IN (:deptIds)
            GROUP BY dept_id
            """, nativeQuery = true)
    List<Object[]> findMaxXhGroupByDeptAndXhlbForDate(
            @org.springframework.data.repository.query.Param("deptIds") List<String> deptIds,
            @org.springframework.data.repository.query.Param("syncDate") java.time.LocalDate syncDate);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM call_list WHERE DATE(call_date) = CURDATE()", nativeQuery = true)
    int deleteAllTodayRows();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM call_list WHERE id IN (:ids)", nativeQuery = true)
    int deleteByIds(@org.springframework.data.repository.query.Param("ids") List<Integer> ids);

    @Query("SELECT DISTINCT c.patientCode, c.patientName FROM CallList c WHERE c.patientCode = :code OR c.patientName LIKE CONCAT('%', :code, '%')")
    List<Object[]> searchPatientsDistinct(@org.springframework.data.repository.query.Param("code") String code);

    /** 护士站：诊区下科室当日排队单 */
    @Query("SELECT c FROM CallList c WHERE c.deptID IN :deptIds AND FUNCTION('DATE', c.callDate) = CURRENT_DATE")
    List<CallList> findTodayByDeptIdIn(@org.springframework.data.repository.query.Param("deptIds") List<String> deptIds);

    @Query("SELECT c FROM CallList c WHERE c.deptID IN :deptIds AND FUNCTION('DATE', c.callDate) = CURRENT_DATE "
            + "AND (c.patientCode LIKE CONCAT('%', :keyword, '%') OR c.showNo LIKE CONCAT('%', :keyword, '%') "
            + "OR c.patientName LIKE CONCAT('%', :keyword, '%'))")
    List<CallList> searchTodayInDepts(@org.springframework.data.repository.query.Param("deptIds") List<String> deptIds,
                                      @org.springframework.data.repository.query.Param("keyword") String keyword);
}

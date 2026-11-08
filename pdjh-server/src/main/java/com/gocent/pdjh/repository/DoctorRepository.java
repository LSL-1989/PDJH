package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, String> {
    List<Doctor> findByDeptIdOrderByTitleAsc(String deptId);

    List<Doctor> findByIsOnDuty(Integer isOnDuty);

    List<Doctor> findByWorkNo(String workNo);

    /** 按 users.user_no 关联查档案（同人同号） */
    List<Doctor> findByUserNo(String userNo);

    /** 按手机号查医生（叫号器登录） */
    List<Doctor> findByPhone(String phone);

    /** 职称改名时同步档案 */
    List<Doctor> findByTitle(String title);

    /** 删除职称前统计仍在使用的医生数 */
    long countByTitle(String title);

    Page<Doctor> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Doctor> findByWorkNoContainingIgnoreCase(String workNo, Pageable pageable);

    Page<Doctor> findByNameContainingIgnoreCaseAndWorkNoContainingIgnoreCase(
            String name, String workNo, Pageable pageable);
}

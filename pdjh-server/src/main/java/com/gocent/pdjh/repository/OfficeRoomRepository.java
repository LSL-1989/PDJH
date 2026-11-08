package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.OfficeRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfficeRoomRepository extends JpaRepository<OfficeRoom, String> {
    List<OfficeRoom> findByDeptId(String deptId);
    List<OfficeRoom> findByState(Integer state);

    /** 当前上岗绑定的诊室/窗口（叫号器默认窗口） */
    List<OfficeRoom> findByDoctorId(String doctorId);
}

package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.Doctor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 医生主数据 CRUD、分页与头像上传（原 screen_intro 能力已合并） */
public interface DoctorService {

    /**
     * 全部医生，并填上 doctor_dept 的科室 id。
     * 没有绑定记录时回退 doctor.dept_id。供排班按所属科室筛诊室，不另开列表接口。
     */
    List<Doctor> listAll();

    Map<String, Object> pageList(Map<String, Object> query);

    Doctor saveFromMap(Map<String, Object> body);

    void deleteById(String id);

    String uploadAvatar(MultipartFile file);
}

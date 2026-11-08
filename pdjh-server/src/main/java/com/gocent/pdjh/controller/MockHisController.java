package com.gocent.pdjh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 欢匠·序流 - 独立隔离的模拟 HIS 接口控制器 (Sandbox Mock Database API)
 * 允许在缺乏真实医院接口时，向系统自身的 HTTP 网关提供无尽的格式化 Dummy 数据，实现网关的内循环测试。
 */
@RestController
@RequestMapping("/mock/his")
public class MockHisController {

    /**
     * 模拟医院标准 RESTful API (例如：卫宁/东软标准的挂号患者全量拉取接口)
     */
    @GetMapping("/patients/today")
    public Map<String, Object> getMockPatients() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("msg", "Mock HIS Data Success");
        
        List<Map<String, Object>> patients = new ArrayList<>();
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 造假数据
        patients.add(createPatient("M-0001", "刘建国", "男", 55, "内科", "NK01", "A015", timeStr));
        patients.add(createPatient("M-0002", "陈雪梅", "女", 28, "妇产科", "FK02", "B002", timeStr));
        patients.add(createPatient("M-0003", "赵铁柱", "男", 40, "骨科", "GK01", "C008", timeStr));
        patients.add(createPatient("M-0004", "王梦琪", "女", 5, "儿科", "EK03", "D033", timeStr));
        
        response.put("data", patients);
        return response;
    }

    /**
     * 模拟医院患者签到/挂号记录拉取接口
     */
    @GetMapping("/checkin/recent")
    public Map<String, Object> getMockCheckins() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        
        List<Map<String, Object>> checkins = new ArrayList<>();
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        checkins.add(createPatient("M-0005", "孙大为", "男", 33, "胸外科", "XK02", "E011", timeStr));
        checkins.add(createPatient("M-0006", "周杰", "男", 29, "泌尿外科", "MK01", "F005", timeStr));

        response.put("data", checkins);
        return response;
    }

    /**
     * 模拟体检中心获取患者当前检查项目状态的接口
     */
    @GetMapping("/exams/status")
    public Map<String, Object> getMockExamStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        
        List<Map<String, Object>> exams = new ArrayList<>();
        Map<String, Object> e1 = new HashMap<>();
        e1.put("patientId", "M-0001");
        e1.put("examItem", "B超");
        e1.put("status", "已完成");
        exams.add(e1);

        Map<String, Object> e2 = new HashMap<>();
        e2.put("patientId", "M-0002");
        e2.put("examItem", "心电图");
        e2.put("status", "等待中");
        exams.add(e2);

        response.put("data", exams);
        return response;
    }

    @GetMapping("/depts")
    public Map<String, Object> getMockDepts() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        List<Map<String, Object>> depts = new ArrayList<>();
        depts.add(dept("NK01", "内科门诊", "OUTPATIENT", null));
        depts.add(dept("FK02", "妇产科", "OUTPATIENT", null));
        depts.add(dept("JY01", "检验科", "LAB", "LAB"));
        depts.add(dept("CS01", "超声科", "MEDTECH", "ULTRASOUND"));
        depts.add(dept("TJ01", "体检中心", "EXAM", null));
        response.put("data", depts);
        return response;
    }

    @GetMapping("/doctors")
    public Map<String, Object> getMockDoctors() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        List<Map<String, Object>> doctors = new ArrayList<>();
        doctors.add(doctor("D001", "张建华", "主任医师", "NK01", "男"));
        doctors.add(doctor("D002", "李秀英", "副主任医师", "FK02", "女"));
        doctors.add(doctor("D003", "王志强", "主治医师", "JY01", "男"));
        response.put("data", doctors);
        return response;
    }

    private Map<String, Object> dept(String code, String name, String businessType, String subType) {
        Map<String, Object> d = new HashMap<>();
        d.put("deptCode", code);
        d.put("deptName", name);
        d.put("businessType", businessType);
        if (subType != null) d.put("medTechSubType", subType);
        return d;
    }

    private Map<String, Object> doctor(String workNo, String name, String title, String deptId, String gender) {
        Map<String, Object> d = new HashMap<>();
        d.put("workNo", workNo);
        d.put("doctorName", name);
        d.put("title", title);
        d.put("deptId", deptId);
        d.put("gender", gender);
        return d;
    }

    private Map<String, Object> createPatient(String id, String name, String gender, int age, String deptName, String deptCode, String seq, String time) {
        Map<String, Object> p = new HashMap<>();
        p.put("patientId", id);
        p.put("patientName", name);
        p.put("gender", gender);
        p.put("age", age);
        p.put("departmentName", deptName);
        p.put("departmentCode", deptCode);
        p.put("sequenceNo", seq);
        p.put("registerTime", time);
        return p;
    }
}

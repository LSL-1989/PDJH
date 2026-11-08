package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.DepartmentInput;
import com.gocent.pdjh.dto.DepartmentOutput;

import java.util.List;

public interface DepartmentService {

    List<DepartmentOutput> getAllDepartment();

    boolean createDepartment(DepartmentInput input);

    boolean editorDepartment(DepartmentInput input);

    boolean removeDepartment(String id);

    /**
     * 根据当前用户所在诊室节点，返回其父级下的诊室簇（导诊台 + 子孙科室）
     */
    List<DepartmentOutput> listDepartmentsForUserParent(String seedDeptId);

    /** 节点无子节点时可挂医生/物理诊室 */
    boolean canAttachResources(String deptId);

    /** 递归收集子孙节点 ID（含多层） */
    void collectDescendants(String parentId, java.util.Set<String> ids);
}

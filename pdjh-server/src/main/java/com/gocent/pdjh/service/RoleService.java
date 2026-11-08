package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.RoleInput;
import com.gocent.pdjh.dto.RoleOutput;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 获取所有角色
     */
    List<RoleOutput> getRoles();

    /**
     * 创建角色
     */
    boolean createRole(RoleInput input);

    /**
     * 编辑角色
     */
    boolean editorRole(RoleInput input);

    /**
     * 删除角色
     */
    boolean removeRole(String roleId);

    /**
     * 获取角色权限（菜单ID列表）
     */
    List<String> getPermissions(String roleId);

    /**
     * 保存角色权限
     */
    boolean commitPermission(String roleId, List<String> pageIds);
}

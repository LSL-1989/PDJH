package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.RoleInput;
import com.gocent.pdjh.dto.RoleOutput;
import com.gocent.pdjh.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/Role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 获取所有角色
     */
    @PostMapping("/GetRole")
    public ResultData<List<RoleOutput>> getRole() {
        try {
            List<RoleOutput> roles = roleService.getRoles();
            return ResultData.success(roles, "获取角色列表成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "获取角色列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据角色ID获取权限
     */
    @PostMapping("/GetPermission")
    public ResultData<List<String>> getPermission(@RequestParam("RoleId") String roleId) {
        try {
            List<String> permissions = roleService.getPermissions(roleId);
            return ResultData.success(permissions, "获取权限成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "获取权限失败: " + e.getMessage());
        }
    }

    /**
     * 创建角色
     */
    @PostMapping("/CreateRole")
    public ResultData<Boolean> createRole(@RequestBody RoleInput input) {
        try {
            boolean ok = roleService.createRole(input);
            return ok ? ResultData.success(true, "创建角色成功") : ResultData.fail(500, "创建角色失败");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "服务器错误，无法新增角色: " + e.getMessage());
        }
    }

    /**
     * 编辑角色
     */
    @PostMapping("/EditorRole")
    public ResultData<Boolean> editorRole(@RequestBody RoleInput input) {
        try {
            boolean ok = roleService.editorRole(input);
            return ok ? ResultData.success(true, "编辑角色成功") : ResultData.fail(500, "编辑角色失败");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "服务器错误，无法修改角色信息: " + e.getMessage());
        }
    }

    /**
     * 删除角色
     */
    @PostMapping("/RemoveRole")
    public ResultData<Boolean> removeRole(@RequestParam("RoleId") String roleId) {
        try {
            boolean ok = roleService.removeRole(roleId);
            return ok ? ResultData.success(true, "删除角色成功")
                    : ResultData.fail(500, "删除失败：角色不存在或仍有用户绑定该角色");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "服务器错误，无法删除角色: " + e.getMessage());
        }
    }

    /**
     * 保存角色权限
     */
    @PostMapping("/CommitPermission")
    public ResultData<Boolean> commitPermission(@RequestBody Map<String, Object> input) {
        try {
            String roleId = (String) input.get("RoleId");
            @SuppressWarnings("unchecked")
            List<String> pages = (List<String>) input.get("Pages");
            boolean ok = roleService.commitPermission(roleId, pages);
            return ok ? ResultData.success(true, "保存权限成功") : ResultData.fail(500, "保存权限失败");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "保存权限失败: " + e.getMessage());
        }
    }
}

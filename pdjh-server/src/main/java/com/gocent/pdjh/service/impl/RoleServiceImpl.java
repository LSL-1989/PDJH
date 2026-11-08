package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.RoleInput;
import com.gocent.pdjh.dto.RoleOutput;
import com.gocent.pdjh.entity.Role;
import com.gocent.pdjh.repository.RoleRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<RoleOutput> getRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(this::convertToOutput).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean createRole(RoleInput input) {
        try {
            if (input == null || input.getRoleName() == null || input.getRoleName().isBlank()) {
                return false;
            }
            Role role = new Role();
            role.setRoleId(UUID.randomUUID().toString().replace("-", ""));
            role.setRoleName(input.getRoleName());
            role.setRoleCode(generateRoleCode(input.getRoleName()));
            role.setIsDelete(input.isDisabledFlag());
            role.setCreateDateTime(LocalDateTime.now());
            role.setRemark(input.getRemark());
            roleRepository.save(role);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    @Transactional
    public boolean editorRole(RoleInput input) {
        try {
            Role role = roleRepository.findById(input.getRoleId()).orElse(null);
            if (role == null) {
                return false;
            }
            role.setRoleName(input.getRoleName());
            role.setIsDelete(input.isDisabledFlag());
            if (input.getRemark() != null) {
                role.setRemark(input.getRemark());
            }
            roleRepository.save(role);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeRole(String roleId) {
        try {
            if (roleId == null || roleId.isBlank()) {
                return false;
            }
            if (userRepository.countByRoleId(roleId) > 0) {
                return false;
            }
            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
            roleRepository.deleteById(roleId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getPermissions(String roleId) {
        // 查询角色关联的菜单ID列表
        try {
            String sql = "SELECT menu_id FROM sys_role_menu WHERE role_id = ?";
            return jdbcTemplate.queryForList(sql, String.class, roleId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public boolean commitPermission(String roleId, List<String> pageIds) {
        // 保存角色菜单权限
        try {
            // 先删除该角色的所有菜单关联
            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
            // 插入新的菜单关联
            if (pageIds != null && !pageIds.isEmpty()) {
                String insertSql = "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)";
                for (String menuId : pageIds) {
                    jdbcTemplate.update(insertSql, roleId, menuId);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据角色名称生成角色编码
     */
    private String generateRoleCode(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return "ROLE_" + System.currentTimeMillis();
        }
        String code = roleName.replaceAll("[^a-zA-Z0-9]", "");
        if (code.isEmpty()) {
            code = "U" + System.currentTimeMillis();
        }
        if (code.length() > 20) {
            code = code.substring(0, 20);
        }
        String candidate = "ROLE_" + code.toUpperCase();
        if (roleRepository.existsByRoleCode(candidate)) {
            return candidate + "_" + (System.currentTimeMillis() % 1_000_000);
        }
        return candidate;
    }

    /**
     * 转换为输出DTO
     */
    private RoleOutput convertToOutput(Role role) {
        RoleOutput output = new RoleOutput();
        output.setRoleId(role.getRoleId());
        output.setRoleName(role.getRoleName());
        output.setRoleCode(role.getRoleCode());
        output.setIsDelete(role.getIsDelete());
        output.setIsDeleteText(role.getIsDelete() ? "禁用" : "启用");
        output.setCreateDateTime(role.getCreateDateTime());
        output.setRemark(role.getRemark());
        return output;
    }
}

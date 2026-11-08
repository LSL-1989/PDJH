package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SysMenuRepository extends JpaRepository<SysMenu, String> {

    /**
     * 查询所有菜单，按排序号升序（所有角色共用，前端做过滤）
     */
    List<SysMenu> findAllByOrderBySortOrderAsc();

    /**
     * 按角色 ID 查询已授权菜单（sys_role_menu 关联，排除已禁用菜单）
     */
    @Query("SELECT m FROM SysMenu m WHERE m.id IN (" +
           "SELECT rm.menuId FROM SysRoleMenu rm WHERE rm.roleId = :roleId) " +
           "AND (m.isDelete IS NULL OR m.isDelete = false) " +
           "ORDER BY m.sortOrder ASC")
    List<SysMenu> findMenusByRoleId(@Param("roleId") String roleId);
}

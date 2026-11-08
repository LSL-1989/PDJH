package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色仓储
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    /**
     * 查询所有角色
     */
    List<Role> findAll();

    /**
     * 根据角色名称查询
     */
    Role findByRoleName(String roleName);

    /**
     * 根据角色编码查询
     */
    Role findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);
}

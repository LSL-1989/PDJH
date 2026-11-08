package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.SysRoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysRoleMenuRepository extends JpaRepository<SysRoleMenu, Long> {

    List<SysRoleMenu> findByRoleId(String roleId);

    void deleteByRoleId(String roleId);

    boolean existsByRoleId(String roleId);
}

package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, String>, JpaSpecificationExecutor<Users> {

    Optional<Users> findByPhone(String phone);

    boolean existsByPhone(String phone);
    
    Optional<Users> findByToken(String token);

    boolean existsByRoleId(String roleId);

    long countByRoleId(String roleId);

    List<Users> findByDeptID(String deptID);

    /** 已申请重置密码的用户（首页待办扫描） */
    List<Users> findByPasswordResetRequested(Integer passwordResetRequested);
}

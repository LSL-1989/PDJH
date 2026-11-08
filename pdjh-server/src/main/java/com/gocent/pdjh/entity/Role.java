package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@Entity
@Table(name = "sys_role")
public class Role {

    @Id
    @Column(name = "role_id", length = 64)
    private String roleId;

    /** 角色名称 */
    @Column(name = "role_name", length = 100, nullable = false)
    private String roleName;

    /** 角色编码 */
    @Column(name = "role_code", length = 50)
    private String roleCode;

    /** 是否禁用：false=启用，true=禁用 */
    @Column(name = "is_delete")
    private Boolean isDelete;

    /** 创建时间 */
    @Column(name = "create_date_time")
    private LocalDateTime createDateTime;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;
}

package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 系统菜单路由实体
 * 对应前端 store 中 createAsyncReoutes 所需的路由结构
 */
@Data
@Entity
@Table(name = "sys_menu")
public class SysMenu {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** 显示名称（侧边栏菜单文字） */
    @Column(name = "page_show_name", length = 100)
    private String pageShowName;

    /** 前端路由 path（如 dashboard、dept/index） */
    @Column(name = "page_url", length = 200)
    private String pageUrl;

    /** 图标名称（element-icon 名称） */
    @Column(name = "icon", length = 100)
    private String icon;

    /** 层级：0 = 一级（父菜单），1 = 二级（子菜单） */
    @Column(name = "level_id")
    private Integer levelId;

    /** 父菜单 id，levelId=0 时填 "0" */
    @Column(name = "parent_page_id", length = 64)
    private String parentPageId;

    /** 是否常驻显示（true=显示在侧栏） */
    @Column(name = "always_show")
    private Boolean alwaysShow;

    /** 是否缓存（true=KeepAlive） */
    @Column(name = "no_cache")
    private Boolean noCache;

    /** 绑定角色（逗号分隔），空代表所有角色可见 */
    @Column(name = "role_codes", length = 200)
    private String roleCodes;

    /** 排序序号 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 是否禁用：false=启用，true=禁用 */
    @Column(name = "is_delete")
    private Boolean isDelete;

    /** 路由重定向（父级菜单可选） */
    @Column(name = "redirect", length = 200)
    private String redirect;
}

package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 菜单输出 DTO（与 Element 表格 / Vue store PascalCase 一致）
 */
@Data
public class PageOutput {
    @JsonProperty("Id")
    private String id;

    @JsonProperty("PageShowName")
    private String pageShowName;

    @JsonProperty("PageUrl")
    private String pageUrl;

    @JsonProperty("Icon")
    private String icon;

    @JsonProperty("LevelId")
    private Integer levelId;

    @JsonProperty("ParentPageId")
    private String parentPageId;

    @JsonProperty("AlwaysShow")
    private Boolean alwaysShow;

    @JsonProperty("NoCache")
    private Boolean noCache;

    @JsonProperty("RoleCodes")
    private String roleCodes;

    @JsonProperty("SortOrder")
    private Integer sortOrder;

    @JsonProperty("IsDelete")
    private Boolean isDelete;

    /** 前端表格列兼容（当前实体无该字段，序列化为 null） */
    @JsonProperty("Redirect")
    private String redirect;
}

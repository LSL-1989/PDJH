package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 菜单输入 DTO（与 maintenanceMenu 表单 PascalCase 一致）
 */
@Data
public class PageInput {
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

    @JsonProperty("Redirect")
    private String redirect;
}

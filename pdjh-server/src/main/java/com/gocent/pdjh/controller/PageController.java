package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.PageInput;
import com.gocent.pdjh.dto.PageOutput;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.SysMenu;
import com.gocent.pdjh.repository.SysMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 菜单管理控制器
 */
@RestController
@RequestMapping("/Page")
@RequiredArgsConstructor
public class PageController {

    private final SysMenuRepository sysMenuRepository;

    /**
     * 获取所有菜单
     */
    @PostMapping("/GetPage")
    public ResultData<List<PageOutput>> getPage() {
        try {
            List<SysMenu> menus = sysMenuRepository.findAllByOrderBySortOrderAsc();
            List<PageOutput> result = menus.stream().map(this::convertToOutput).collect(Collectors.toList());
            return ResultData.success(result, "获取菜单成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "获取菜单失败: " + e.getMessage());
        }
    }

    /**
     * 创建菜单
     */
    @PostMapping("/CreatePage")
    public ResultData<Boolean> createPage(@RequestBody PageInput input) {
        try {
            SysMenu menu = new SysMenu();
            menu.setId(UUID.randomUUID().toString().replace("-", ""));
            menu.setPageShowName(input.getPageShowName());
            menu.setPageUrl(input.getPageUrl());
            menu.setIcon(input.getIcon());
            String parentId = input.getParentPageId() != null ? input.getParentPageId() : "0";
            menu.setParentPageId(parentId);
            menu.setLevelId(resolveLevelId(parentId, input.getLevelId()));
            menu.setAlwaysShow(input.getAlwaysShow());
            menu.setNoCache(input.getNoCache());
            menu.setRoleCodes(input.getRoleCodes());
            menu.setSortOrder(input.getSortOrder() != null ? input.getSortOrder() : 0);
            menu.setIsDelete(Boolean.TRUE.equals(input.getIsDelete()));
            menu.setRedirect(input.getRedirect());
            sysMenuRepository.save(menu);
            return ResultData.success(true, "创建菜单成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "创建菜单失败: " + e.getMessage());
        }
    }

    /**
     * 编辑菜单
     */
    @PostMapping("/EditorPage")
    public ResultData<Boolean> editorPage(@RequestBody PageInput input) {
        try {
            SysMenu menu = sysMenuRepository.findById(input.getId()).orElse(null);
            if (menu == null) {
                return ResultData.fail(404, "菜单不存在");
            }
            if (input.getPageShowName() != null) {
                menu.setPageShowName(input.getPageShowName());
            }
            if (input.getPageUrl() != null) {
                menu.setPageUrl(input.getPageUrl());
            }
            if (input.getIcon() != null) {
                menu.setIcon(input.getIcon());
            }
            if (input.getParentPageId() != null) {
                menu.setParentPageId(input.getParentPageId());
                menu.setLevelId(resolveLevelId(input.getParentPageId(), input.getLevelId()));
            } else if (input.getLevelId() != null) {
                menu.setLevelId(input.getLevelId());
            }
            if (input.getAlwaysShow() != null) {
                menu.setAlwaysShow(input.getAlwaysShow());
            }
            if (input.getNoCache() != null) {
                menu.setNoCache(input.getNoCache());
            }
            if (input.getRoleCodes() != null) {
                menu.setRoleCodes(input.getRoleCodes());
            }
            if (input.getSortOrder() != null) {
                menu.setSortOrder(input.getSortOrder());
            }
            if (input.getIsDelete() != null) {
                menu.setIsDelete(input.getIsDelete());
            }
            if (input.getRedirect() != null) {
                menu.setRedirect(input.getRedirect());
            }
            sysMenuRepository.save(menu);
            return ResultData.success(true, "编辑菜单成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "编辑菜单失败: " + e.getMessage());
        }
    }

    /**
     * 删除菜单
     */
    @PostMapping("/RemovePage")
    public ResultData<Boolean> removePage(@RequestParam("id") String id) {
        try {
            sysMenuRepository.deleteById(id);
            return ResultData.success(true, "删除菜单成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.fail(500, "删除菜单失败: " + e.getMessage());
        }
    }

    /**
     * 转换为输出DTO
     */
    private PageOutput convertToOutput(SysMenu menu) {
        PageOutput output = new PageOutput();
        output.setId(menu.getId());
        output.setPageShowName(menu.getPageShowName());
        output.setPageUrl(menu.getPageUrl());
        output.setIcon(menu.getIcon());
        output.setLevelId(menu.getLevelId());
        output.setParentPageId(menu.getParentPageId());
        output.setAlwaysShow(menu.getAlwaysShow());
        output.setNoCache(menu.getNoCache());
        output.setRoleCodes(menu.getRoleCodes());
        output.setSortOrder(menu.getSortOrder());
        output.setIsDelete(Boolean.TRUE.equals(menu.getIsDelete()));
        output.setRedirect(menu.getRedirect());
        return output;
    }

    /** 前端未传 LevelId 时：顶级 parent=0 为 0，否则为子级 1（与 store 两级树一致） */
    private static int resolveLevelId(String parentPageId, Integer explicit) {
        if (explicit != null) {
            return explicit;
        }
        if (parentPageId == null || parentPageId.isBlank() || "0".equalsIgnoreCase(parentPageId.trim())) {
            return 0;
        }
        return 1;
    }
}

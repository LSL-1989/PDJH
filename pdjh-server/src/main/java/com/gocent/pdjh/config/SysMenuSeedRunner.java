package com.gocent.pdjh.config;

import com.gocent.pdjh.entity.SysMenu;
import com.gocent.pdjh.repository.SysMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 首次部署或升级时写入/补全 sys_menu（与前端 router 一致，可继续在菜单管理中增删）。
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class SysMenuSeedRunner implements ApplicationRunner {

    public static final String ID_HOME = "00000000000000000000000000000001";
    public static final String ID_DIRECTOR = "00000000000000000000000000000003";
    public static final String ID_DEVICE = "00000000000000000000000000000004";
    public static final String ID_SCREEN_CFG = "00000000000000000000000000000005";
    public static final String ID_CLINIC_CFG = "0000000000000000000000000000000a";
    public static final String ID_HOSPITAL = "00000000000000000000000000000006";
    public static final String ID_BUSINESS = "00000000000000000000000000000007";
    public static final String ID_INTEGRATION = "00000000000000000000000000000008";
    public static final String ID_IMPLEMENTATION = "00000000000000000000000000000009";
    public static final String ID_SYS_GROUP = "00000000000000000000000000000002";
    public static final String ID_MENU_USER = "00000000000000000000000000000011";
    public static final String ID_MENU_ROLE = "00000000000000000000000000000012";
    public static final String ID_MENU_PAGE = "00000000000000000000000000000013";

    private final SysMenuRepository sysMenuRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SysMenu> seeds = buildSeedMenus();
        Map<String, SysMenu> existing = sysMenuRepository.findAll().stream()
                .collect(Collectors.toMap(SysMenu::getId, Function.identity(), (a, b) -> a));

        List<SysMenu> toSave = new ArrayList<>();
        int added = 0;
        for (SysMenu seed : seeds) {
            if (!existing.containsKey(seed.getId())) {
                toSave.add(seed);
                added++;
            }
        }
        if (!toSave.isEmpty()) {
            sysMenuRepository.saveAll(toSave);
            log.info("sys_menu 补全 {} 条默认菜单（共定义 {} 条）", added, seeds.size());
        }
        relocateDoctorTitleMenu();
        repairSharedIndexPageUrls();
    }

    /**
     * 已部署库里候诊大屏、门边屏的子菜单 page_url 都是 index。
     * 侧栏按 page_url 套标题，后写入的名称会盖掉前一条，两条都显示成「门边屏显示配置」。
     * 种子只插缺失行，所以这里把仍为 index 的子菜单改成各自的路由 name。
     */
    private void repairSharedIndexPageUrls() {
        List<SysMenu> all = sysMenuRepository.findAll();
        Map<String, SysMenu> byId = all.stream()
                .collect(Collectors.toMap(SysMenu::getId, Function.identity(), (a, b) -> a));
        for (SysMenu menu : all) {
            if (menu.getPageUrl() == null || !"index".equalsIgnoreCase(menu.getPageUrl().trim())) {
                continue;
            }
            SysMenu parent = byId.get(menu.getParentPageId());
            if (parent == null || parent.getPageUrl() == null) {
                continue;
            }
            String mapped = indexChildPageUrl(parent.getPageUrl().trim());
            if (mapped == null) {
                continue;
            }
            menu.setPageUrl(mapped);
            sysMenuRepository.save(menu);
            log.info("菜单「{}」page_url 由 index 改为 {}，避免侧栏标题串名", menu.getPageShowName(), mapped);
        }
    }

    /** 父级 page_url → 子菜单应使用的路由 name；未知父级返回 null，不改人工菜单 */
    private static String indexChildPageUrl(String parentPageUrl) {
        if ("screenConfig".equalsIgnoreCase(parentPageUrl)) {
            return "screenConfigIndex";
        }
        if ("clinicConfig".equalsIgnoreCase(parentPageUrl)) {
            return "clinicConfigIndex";
        }
        return null;
    }

    /** 已部署库里的医生职称仍挂在院内业务树时，归入系统与权限管理。种子只插缺失行，所以要单独改父级。 */
    private void relocateDoctorTitleMenu() {
        sysMenuRepository.findById("00000000000000000000000000000064").ifPresent(menu -> {
            if (!ID_SYS_GROUP.equals(menu.getParentPageId())) {
                menu.setParentPageId(ID_SYS_GROUP);
                menu.setSortOrder(16);
                sysMenuRepository.save(menu);
                log.info("医生职称菜单已归入系统与权限管理");
            }
        });
    }

    private static List<SysMenu> buildSeedMenus() {
        List<SysMenu> list = new ArrayList<>();
        list.add(row(ID_HOME, "首页", "dashboard", "Home", 0, "0", true, false, 1, null));

        list.add(row(ID_DIRECTOR, "主任工作台", "directorDesk", "el-icon-s-custom", 0, "0", true, false, 2, "/directorDesk/outpatientDesk"));
        list.add(row("00000000000000000000000000000031", "门诊主任工作台", "outpatientDesk", null, 1, ID_DIRECTOR, true, false, 31, null));
        list.add(row("00000000000000000000000000000032", "药房主任工作台", "pharmacyDesk", null, 1, ID_DIRECTOR, true, false, 32, null));
        list.add(row("00000000000000000000000000000033", "体检中心主任工作台", "examDesk", null, 1, ID_DIRECTOR, true, false, 33, null));
        list.add(row("00000000000000000000000000000034", "检验主任工作台", "labDesk", null, 1, ID_DIRECTOR, true, false, 34, null));
        list.add(row("00000000000000000000000000000035", "医技主任工作台", "medtechDesk", null, 1, ID_DIRECTOR, true, false, 35, null));
        list.add(row("00000000000000000000000000000036", "全院排队统计分析", "statistics", "el-icon-s-data", 1, ID_DIRECTOR, true, false, 36, null));

        list.add(row(ID_DEVICE, "设备与屏幕", "deviceManage", "el-icon-monitor", 0, "0", true, false, 3, "/deviceManage/ScreenManages"));
        list.add(row("00000000000000000000000000000041", "屏幕管理", "ScreenManages", null, 1, ID_DEVICE, true, false, 41, null));
        list.add(row("00000000000000000000000000000043", "签到终端管理", "SignDeviceManages", null, 1, ID_DEVICE, true, false, 43, null));
        list.add(row("00000000000000000000000000000044", "独立音响管理", "VoiceDeviceManages", null, 1, ID_DEVICE, true, false, 44, null));
        list.add(row("00000000000000000000000000000042", "编号规则管理", "AutoNumManages", null, 1, ID_DEVICE, true, false, 42, null));

        list.add(row(ID_SCREEN_CFG, "大屏显示内容配置", "screenConfig", "el-icon-video-camera", 0, "0", true, false, 4, "/screenConfig/index"));
        // page_url 用路由 name，不用 path「index」：与门边屏子菜单撞键后，侧栏标题和权限会串到同一条
        list.add(row("00000000000000000000000000000051", "候诊区大屏配置", "screenConfigIndex", null, 1, ID_SCREEN_CFG, true, false, 51, null));

        list.add(row(ID_CLINIC_CFG, "门边屏显示配置", "clinicConfig", "el-icon-mobile-phone", 0, "0", true, false, 45, "/clinicConfig/index"));
        list.add(row("0000000000000000000000000000000a1", "门边屏显示配置", "clinicConfigIndex", null, 1, ID_CLINIC_CFG, true, false, 451, null));

        list.add(row(ID_HOSPITAL, "院内业务树", "hospitalInfo", "el-icon-office-building", 0, "0", true, false, 5, "/hospitalInfo/DeptManages"));
        list.add(row("00000000000000000000000000000061", "科室管理", "DeptManages", null, 1, ID_HOSPITAL, true, false, 61, null));
        list.add(row("00000000000000000000000000000062", "物理诊室与终端资源", "OfficeRoomManages", null, 1, ID_HOSPITAL, true, false, 62, null));
        list.add(row("00000000000000000000000000000063", "医生管理", "DoctorProfile", null, 1, ID_HOSPITAL, true, false, 63, null));

        list.add(row(ID_BUSINESS, "业务配置中心", "businessConfig", "el-icon-s-tools", 0, "0", true, false, 6, "/businessConfig/ruleCenter"));
        list.add(row("00000000000000000000000000000071", "叫号规则配置中心", "ruleCenter", null, 1, ID_BUSINESS, true, false, 71, null));
        list.add(row("00000000000000000000000000000072", "出诊排班管理", "doctorSchedule", null, 1, ID_BUSINESS, true, false, 72, null));
        list.add(row("00000000000000000000000000000075", "护士当班排班", "nurseSchedule", null, 1, ID_BUSINESS, true, false, 75, null));
        list.add(row("00000000000000000000000000000073", "体检套餐管理", "examPackage", null, 1, ID_BUSINESS, true, false, 73, null));
        list.add(row("00000000000000000000000000000074", "医技预约时段配置", "medtechAppointment", null, 1, ID_BUSINESS, true, false, 74, null));

        list.add(row(ID_INTEGRATION, "集成网关管理", "systemIntegration", "el-icon-connection", 0, "0", true, false, 7, "/systemIntegration/integrationConfig"));
        list.add(row("00000000000000000000000000000081", "数据源接口配置", "integrationConfig", null, 1, ID_INTEGRATION, true, false, 81, null));
        list.add(row("00000000000000000000000000000082", "运维护航沙盘", "integrationDashboard", "el-icon-odometer", 1, ID_INTEGRATION, true, false, 82, null));
        list.add(row("00000000000000000000000000000083", "外部网关审计查询", "auditLog", null, 1, ID_INTEGRATION, true, false, 83, null));
        list.add(row("00000000000000000000000000000084", "业务事件日志轨迹", "systemLog", null, 1, ID_INTEGRATION, true, false, 84, null));
        list.add(row("00000000000000000000000000000085", "患者流程日志", "patientFlowLog", null, 1, ID_INTEGRATION, true, false, 85, null));

        list.add(row(ID_IMPLEMENTATION, "业务实施中心", "implementation", "el-icon-guide", 0, "0", true, false, 8, "/implementation/wizard"));
        list.add(row("00000000000000000000000000000091", "业务实施中心", "wizard", null, 1, ID_IMPLEMENTATION, true, false, 91, null));

        list.add(row(ID_SYS_GROUP, "系统与权限管理", "userManage", "el-icon-s-operation", 0, "0", true, false, 9, "/userManage/systemConfig"));
        list.add(row("00000000000000000000000000000014", "全局字典参数", "systemConfig", null, 1, ID_SYS_GROUP, true, false, 14, null));
        list.add(row("00000000000000000000000000000015", "排班类型", "shiftType", null, 1, ID_SYS_GROUP, true, false, 15, null));
        list.add(row("00000000000000000000000000000064", "医生职称", "DoctorTitle", null, 1, ID_SYS_GROUP, true, false, 16, null));
        list.add(row(ID_MENU_USER, "用户管理", "maintenanceUser", "user", 1, ID_SYS_GROUP, true, false, 11, null));
        list.add(row(ID_MENU_ROLE, "角色管理", "maintenanceRole", "peoples", 1, ID_SYS_GROUP, true, false, 12, null));
        list.add(row(ID_MENU_PAGE, "菜单管理", "maintenanceMenu", "tree-table", 1, ID_SYS_GROUP, true, false, 13, null));

        return list;
    }

    private static SysMenu row(String id, String showName, String pageUrl, String icon, int levelId,
                               String parentPageId, boolean alwaysShow, boolean noCache, int sortOrder, String redirect) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setPageShowName(showName);
        m.setPageUrl(pageUrl);
        m.setIcon(icon);
        m.setLevelId(levelId);
        m.setParentPageId(parentPageId);
        m.setAlwaysShow(alwaysShow);
        m.setNoCache(noCache);
        m.setRoleCodes(null);
        m.setSortOrder(sortOrder);
        m.setIsDelete(false);
        m.setRedirect(redirect);
        return m;
    }
}

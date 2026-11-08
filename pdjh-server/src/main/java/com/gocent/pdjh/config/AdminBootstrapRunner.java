package com.gocent.pdjh.config;

import com.gocent.pdjh.entity.Role;
import com.gocent.pdjh.entity.SysMenu;
import com.gocent.pdjh.entity.SysRoleMenu;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.LoginScene;
import com.gocent.pdjh.repository.RoleRepository;
import com.gocent.pdjh.repository.SysMenuRepository;
import com.gocent.pdjh.repository.SysRoleMenuRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.util.PasswordHashes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 建表完成后的默认数据：系统管理员角色、全量菜单授权、超管 wuaitecadmin。
 * 超管密码经 BCrypt 加密后入库，明文不写 SQL、不落库。
 */
@Slf4j
@Component
@Order(101)
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    public static final String ADMIN_ROLE_ID = "admin";
    public static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";
    public static final String ADMIN_USER_NO = "wuaitecadmin";
    public static final String ADMIN_DEFAULT_PASSWORD = "wuaitec@123";

    private final RoleRepository roleRepository;
    private final SysMenuRepository sysMenuRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role adminRole = ensureAdminRole();
        syncRoleMenus(adminRole.getRoleId());
        ensureAdminUser(adminRole);
        upgradePlaintextPasswords();
    }

    private Role ensureAdminRole() {
        return roleRepository.findById(ADMIN_ROLE_ID).orElseGet(() -> {
            Role role = roleRepository.findByRoleCode(ADMIN_ROLE_CODE);
            if (role != null) {
                return role;
            }
            log.info("写入默认系统管理员角色");
            Role created = new Role();
            created.setRoleId(ADMIN_ROLE_ID);
            created.setRoleName("系统管理员");
            created.setRoleCode(ADMIN_ROLE_CODE);
            created.setIsDelete(false);
            created.setCreateDateTime(LocalDateTime.now());
            created.setRemark("启动种子：拥有全部已配置菜单");
            return roleRepository.save(created);
        });
    }

    private void syncRoleMenus(String roleId) {
        List<SysMenu> menus = sysMenuRepository.findAllByOrderBySortOrderAsc();
        if (menus.isEmpty()) {
            return;
        }
        Set<String> existingMenuIds = new HashSet<>();
        sysRoleMenuRepository.findByRoleId(roleId).forEach(rm -> existingMenuIds.add(rm.getMenuId()));
        int added = 0;
        for (SysMenu menu : menus) {
            if (Boolean.TRUE.equals(menu.getIsDelete())) {
                continue;
            }
            if (!existingMenuIds.contains(menu.getId())) {
                SysRoleMenu link = new SysRoleMenu();
                link.setRoleId(roleId);
                link.setMenuId(menu.getId());
                sysRoleMenuRepository.save(link);
                added++;
            }
        }
        if (added > 0) {
            log.info("系统管理员角色已同步 {} 条菜单权限", added);
        }
    }

    /**
     * 缺省超管：Hibernate 建好 users 表后写入；密码只存 BCrypt。
     */
    private void ensureAdminUser(Role adminRole) {
        Users admin = userRepository.findById(ADMIN_USER_NO).orElse(null);
        if (admin == null) {
            log.info("写入默认超管 wuaitecadmin（role_id={}）", adminRole.getRoleId());
            Users created = new Users();
            created.setUserNo(ADMIN_USER_NO);
            created.setUserName("系统管理员");
            created.setPassWord(PasswordHashes.hash(ADMIN_DEFAULT_PASSWORD));
            created.setState(1);
            created.setDeptID(null);
            created.setRoleId(adminRole.getRoleId());
            created.setRole(adminRole.getRoleCode());
            created.setSignCode("SIGN_ADMIN");
            created.setLoginScene(LoginScene.ADMIN.getCode());
            created.setMustChangePassword(0);
            created.setPasswordResetRequested(0);
            created.setCreateDate(new Date());
            userRepository.save(created);
            return;
        }
        boolean changed = false;
        if (admin.getRoleId() == null || admin.getRoleId().isBlank()) {
            admin.setRoleId(adminRole.getRoleId());
            admin.setRole(adminRole.getRoleCode());
            changed = true;
        }
        if (admin.getState() == null) {
            admin.setState(1);
            changed = true;
        }
        if (admin.getLoginScene() == null || admin.getLoginScene().isBlank()) {
            admin.setLoginScene(LoginScene.ADMIN.getCode());
            changed = true;
        }
        if (changed) {
            userRepository.save(admin);
            log.info("已补全超管 wuaitecadmin 的角色绑定");
        }
    }

    /** 启动时把库里遗留明文密码改为 BCrypt，避免明文长期留存 */
    private void upgradePlaintextPasswords() {
        for (Users user : userRepository.findAll()) {
            if (user.getPassWord() != null && !PasswordHashes.isHashed(user.getPassWord())) {
                user.setPassWord(PasswordHashes.hash(user.getPassWord()));
                userRepository.save(user);
                log.info("已将用户 {} 的明文密码升级为 BCrypt", user.getUserNo());
            }
        }
    }
}

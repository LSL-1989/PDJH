package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.PasswordChangeInput;
import com.gocent.pdjh.dto.TokenInput;
import com.gocent.pdjh.dto.TokenOutput;
import com.gocent.pdjh.dto.UserInput;
import com.gocent.pdjh.dto.UserOutput;
import com.gocent.pdjh.entity.Role;
import com.gocent.pdjh.entity.SysMenu;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.LoginScene;
import com.gocent.pdjh.exception.LoginDeniedException;
import com.gocent.pdjh.exception.SessionExpiredException;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.NurseScheduleRepository;
import com.gocent.pdjh.repository.RoleRepository;
import com.gocent.pdjh.repository.SysMenuRepository;
import com.gocent.pdjh.repository.SysRoleMenuRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.AdminSessionService;
import com.gocent.pdjh.service.LoginLogService;
import com.gocent.pdjh.service.UserService;
import com.gocent.pdjh.util.PasswordHashes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SysMenuRepository sysMenuRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final RoleRepository roleRepository;
    private final CallListRepository callListRepository;
    private final LoginLogService loginLogService;
    private final NurseScheduleRepository nurseScheduleRepository;
    private final AdminSessionService adminSessionService;

    @Override
    public TokenOutput loginUser(TokenInput input) {
        if (input == null || input.getUserNo() == null || input.getPassword() == null) {
            throw new LoginDeniedException("用户名或密码不能为空");
        }

        String account = input.getUserNo().trim();
        String password = input.getPassword();
        Users user = userRepository.findById(account)
                .or(() -> userRepository.findByPhone(account))
                .orElse(null);
        if (user == null || !PasswordHashes.matches(password, user.getPassWord())) {
            log.warn("登录失败: 用户名或密码错误 - {}", account);
            loginLogService.record(account, user != null ? user.getLoginScene() : "ADMIN", false, "用户名或密码错误");
            throw new LoginDeniedException("用户名或密码错误");
        }
        if (!PasswordHashes.isHashed(user.getPassWord())) {
            user.setPassWord(PasswordHashes.hash(password));
        }

        try {
            assertWebLoginScene(user, input.getLoginScene());
        } catch (LoginDeniedException e) {
            loginLogService.record(user.getUserNo(), user.getLoginScene(), false, e.getMessage());
            throw e;
        }

        Role role;
        try {
            role = validateUserForLogin(user);
        } catch (LoginDeniedException e) {
            loginLogService.record(user.getUserNo(), user.getLoginScene(), false, e.getMessage());
            throw e;
        }
        String newToken = UUID.randomUUID().toString().replace("-", "");
        user.setToken(newToken);
        user.setTokenTime(new Date());
        userRepository.save(user);
        loginLogService.record(user.getUserNo(),
                user.getLoginScene() != null ? user.getLoginScene() : "ADMIN", true, null);

        return buildTokenOutput(user, role);
    }

    @Override
    public TokenOutput getInfo(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        // 打开/刷新页面算一次操作：未过期则续期，超过 30 分钟则作废 token
        if (adminSessionService.checkToken(token, true) == AdminSessionService.Check.EXPIRED) {
            throw new SessionExpiredException(AdminSessionService.EXPIRED_MSG);
        }
        Users user = userRepository.findByToken(token).orElse(null);
        if (user == null) {
            throw new SessionExpiredException(AdminSessionService.EXPIRED_MSG);
        }
        Role role = validateUserForLogin(user);
        return buildTokenOutput(user, role);
    }

    @Override
    public boolean logOut(String userNo) {
        Users user = userRepository.findById(userNo).orElse(null);
        if (user != null) {
            user.setToken("");
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public TokenOutput getUserInfo(TokenInput input) {
        return loginUser(input);
    }

    @Override
    public boolean addUser(UserInput input) {
        if (input == null || input.getUserNo() == null || input.getUserNo().isBlank()) {
            return false;
        }
        if (input.getRoleId() == null || input.getRoleId().isBlank()) {
            log.warn("创建失败，未指定角色: {}", input.getUserNo());
            return false;
        }
        LoginScene scene = LoginScene.fromCode(input.getLoginScene());
        if (scene == null) {
            log.warn("创建失败，登录场景无效: {}", input.getLoginScene());
            return false;
        }
        try {
            String userNo = input.getUserNo().trim();
            if (userRepository.existsById(userNo)) {
                log.warn("创建失败，用户编码已存在: {}", userNo);
                return false;
            }
            String phone = normalizePhone(input.getPhone());
            if (phone != null && userRepository.existsByPhone(phone)) {
                log.warn("创建失败，手机号已存在: {}", phone);
                return false;
            }
            Role role = roleRepository.findById(input.getRoleId()).orElse(null);
            if (role == null || Boolean.TRUE.equals(role.getIsDelete())) {
                log.warn("创建失败，角色无效: {}", input.getRoleId());
                return false;
            }
            Users user = new Users();
            BeanUtils.copyProperties(input, user);
            user.setUserNo(userNo);
            user.setPhone(phone);
            user.setLoginScene(scene.getCode());
            if (input.getDeptId() != null && !input.getDeptId().isBlank()) {
                user.setDeptID(input.getDeptId());
            }
            String pwd = input.getPassWord();
            if (pwd == null || pwd.isBlank()) {
                pwd = Users.DEFAULT_PASSWORD;
            }
            user.setPassWord(PasswordHashes.hash(pwd));
            if (user.getState() == null) {
                user.setState(1);
            }
            user.setMustChangePassword(1);
            user.setPasswordResetRequested(0);
            user.setRoleId(input.getRoleId());
            user.setRole(role.getRoleCode());
            user.setCreateDate(new Date());
            userRepository.save(user);
            log.info("新增用户成功: {} scene={}", user.getUserNo(), user.getLoginScene());
            return true;
        } catch (Exception e) {
            log.error("新增用户失败", e);
            return false;
        }
    }

    @Override
    public boolean editorUser(UserInput input) {
        if (input == null || input.getUserNo() == null) {
            return false;
        }
        Users existing = userRepository.findById(input.getUserNo()).orElse(null);
        if (existing == null) {
            return false;
        }
        if (input.getUserName() != null) {
            existing.setUserName(input.getUserName());
        }
        if (input.getDeptId() != null) {
            existing.setDeptID(input.getDeptId().isBlank() ? null : input.getDeptId());
        }
        if (input.getState() != null) {
            existing.setState(input.getState());
        }
        if (input.getLoginScene() != null) {
            LoginScene scene = LoginScene.fromCode(input.getLoginScene());
            if (scene == null) {
                log.warn("修改失败，登录场景无效: {}", input.getLoginScene());
                return false;
            }
            existing.setLoginScene(scene.getCode());
        }
        if (input.getPhone() != null) {
            String phone = normalizePhone(input.getPhone());
            if (phone != null) {
                Optional<Users> byPhone = userRepository.findByPhone(phone);
                if (byPhone.isPresent() && !byPhone.get().getUserNo().equals(existing.getUserNo())) {
                    log.warn("修改失败，手机号已被占用: {}", phone);
                    return false;
                }
            }
            existing.setPhone(phone);
        }
        if (input.getRoleId() != null && !input.getRoleId().isBlank()) {
            Role role = roleRepository.findById(input.getRoleId()).orElse(null);
            if (role == null || Boolean.TRUE.equals(role.getIsDelete())) {
                log.warn("修改失败，角色无效: {}", input.getRoleId());
                return false;
            }
            existing.setRoleId(input.getRoleId());
            existing.setRole(role.getRoleCode());
        }
        userRepository.save(existing);
        log.info("修改用户成功: {}", existing.getUserNo());
        return true;
    }

    @Override
    public boolean removeUser(String userNo) {
        try {
            nurseScheduleRepository.deleteByUserNo(userNo);
            userRepository.deleteById(userNo);
            log.info("删除用户成功: {}", userNo);
            return true;
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getUsers(int pageIndex, int pageSize, String keyword, String roleId,
                                        String loginScene, Integer passwordResetRequested) {
        int page = Math.max(pageIndex, 1);
        int size = pageSize > 0 ? pageSize : 20;
        Specification<Users> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("userNo"), like),
                        cb.like(root.get("userName"), like),
                        cb.like(root.get("phone"), like)));
            }
            if (roleId != null && !roleId.isBlank()) {
                predicates.add(cb.equal(root.get("roleId"), roleId.trim()));
            }
            if (loginScene != null && !loginScene.isBlank()) {
                predicates.add(cb.equal(root.get("loginScene"), loginScene.trim().toUpperCase()));
            }
            if (passwordResetRequested != null) {
                predicates.add(cb.equal(root.get("passwordResetRequested"), passwordResetRequested));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Users> result = userRepository.findAll(
                spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createDate")));
        List<UserOutput> rows = result.getContent().stream().map(this::toUserOutput).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", rows);
        body.put("pageInfo", Map.of("totalCount", result.getTotalElements()));
        return body;
    }

    private UserOutput toUserOutput(Users user) {
        UserOutput out = new UserOutput();
        out.setUserNo(user.getUserNo());
        out.setUserName(user.getUserName());
        out.setPhone(user.getPhone());
        out.setLoginScene(user.getLoginScene());
        LoginScene scene = LoginScene.fromCode(user.getLoginScene());
        out.setLoginSceneName(scene != null ? scene.getLabel() : user.getLoginScene());
        out.setPasswordResetRequested(user.getPasswordResetRequested() != null ? user.getPasswordResetRequested() : 0);
        out.setPasswordResetRequestTime(user.getPasswordResetRequestTime());
        out.setCreateDate(user.getCreateDate());
        out.setState(user.getState());
        out.setDeptID(user.getDeptID());
        out.setRoleId(user.getRoleId());
        out.setSignCode(user.getSignCode());
        String roleDisplay = user.getRole();
        if (user.getRoleId() != null && !user.getRoleId().isBlank()) {
            roleRepository.findById(user.getRoleId()).ifPresent(r -> {
                out.setRoleName(r.getRoleName());
                if (r.getRoleName() != null && !r.getRoleName().isBlank()) {
                    out.setRole(r.getRoleName());
                }
            });
        }
        if (out.getRole() == null) {
            out.setRole(roleDisplay);
        }
        return out;
    }

    /** 空串视为未填手机号 */
    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }

    @Override
    public Map<String, String> getUserClinicDept(String userNo) {
        if (userNo == null || userNo.isEmpty()) {
            return Map.of("DeptId", "");
        }
        return userRepository.findById(userNo)
                .map(u -> Map.of("DeptId", u.getDeptID() != null ? u.getDeptID() : ""))
                .orElse(Map.of("DeptId", ""));
    }

    @Override
    public List<Map<String, String>> getPatientByCardNo(String patientCardNo) {
        if (patientCardNo == null || patientCardNo.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = callListRepository.searchPatientsDistinct(patientCardNo.trim());
        List<Map<String, String>> out = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("PatientCode", row[0] != null ? row[0].toString() : "");
            m.put("PatientName", row[1] != null ? row[1].toString() : "");
            out.add(m);
        }
        return out;
    }

    @Override
    public boolean resetPassword(PasswordChangeInput input) {
        if (input == null || input.getUserNo() == null || input.getOldPwd() == null || input.getNewPwd() == null) {
            return false;
        }
        Users u = userRepository.findById(input.getUserNo()).orElse(null);
        if (u == null || u.getPassWord() == null || !PasswordHashes.matches(input.getOldPwd(), u.getPassWord())) {
            return false;
        }
        u.setPassWord(PasswordHashes.hash(input.getNewPwd()));
        u.setMustChangePassword(0);
        u.setPasswordResetRequested(0);
        u.setPasswordResetRequestTime(null);
        userRepository.save(u);
        return true;
    }

    @Override
    public void requestPasswordReset(String account) {
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("请输入帐号/工号或手机号");
        }
        String key = account.trim();
        Users user = userRepository.findById(key).orElse(null);
        if (user == null) {
            user = userRepository.findByPhone(key).orElse(null);
        }
        if (user == null) {
            throw new IllegalArgumentException("未找到该帐号/工号或手机号对应的用户");
        }
        if (user.getState() != null && user.getState() != 1) {
            throw new IllegalArgumentException("账号已禁用，请联系管理员");
        }
        user.setPasswordResetRequested(1);
        user.setPasswordResetRequestTime(new Date());
        userRepository.save(user);
        log.info("用户申请重置密码: {}", user.getUserNo());
    }

    @Override
    public void adminResetPassword(String userNo) {
        if (userNo == null || userNo.isBlank()) {
            throw new IllegalArgumentException("帐号不能为空");
        }
        Users user = userRepository.findById(userNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setPassWord(PasswordHashes.hash(Users.DEFAULT_PASSWORD));
        user.setMustChangePassword(1);
        user.setPasswordResetRequested(0);
        user.setPasswordResetRequestTime(null);
        userRepository.save(user);
        log.info("管理员重置用户密码: {}", user.getUserNo());
    }

    /**
     * 管理后台 / 护士站登录场景校验（§23.8）。
     * 叫号器岗位拒绝走本接口；传入 expectedScene 时须与 users.login_scene 一致。
     * 未传 expectedScene 时 ADMIN 与 NURSE 均可（兼容旧客户端）。
     */
    private void assertWebLoginScene(Users user, String expectedScene) {
        LoginScene actual = LoginScene.fromCode(user.getLoginScene());
        if (actual == null) {
            actual = LoginScene.ADMIN;
        }
        if (actual.isCallStation()) {
            throw new LoginDeniedException("该账号请使用叫号器登录");
        }
        LoginScene expected = LoginScene.fromCode(expectedScene);
        if (expected == null) {
            return;
        }
        if (expected.isCallStation()) {
            throw new LoginDeniedException("该账号请使用叫号器登录");
        }
        if (actual != expected) {
            throw new LoginDeniedException("该账号不能登录当前系统");
        }
    }

    private Role validateUserForLogin(Users user) {
        if (user.getState() == null || user.getState() != 1) {
            throw new LoginDeniedException("账号已禁用");
        }
        if (user.getRoleId() == null || user.getRoleId().isBlank()) {
            throw new LoginDeniedException("未分配角色，请联系管理员");
        }
        Role role = roleRepository.findById(user.getRoleId()).orElse(null);
        if (role == null) {
            throw new LoginDeniedException("角色不存在或已被删除");
        }
        if (Boolean.TRUE.equals(role.getIsDelete())) {
            throw new LoginDeniedException("角色已禁用");
        }
        if (!sysRoleMenuRepository.existsByRoleId(user.getRoleId())) {
            throw new LoginDeniedException("角色未配置任何菜单权限");
        }
        List<SysMenu> menus = sysMenuRepository.findMenusByRoleId(user.getRoleId());
        if (menus.isEmpty()) {
            throw new LoginDeniedException("角色未配置任何可用菜单");
        }
        return role;
    }

    private TokenOutput buildTokenOutput(Users user, Role role) {
        TokenOutput output = new TokenOutput();
        output.setUserNo(user.getUserNo());
        output.setUserName(user.getUserName());
        output.setDeptId(user.getDeptID());
        output.setToken(user.getToken());
        output.setRole(user.getRole());
        output.setRoleId(user.getRoleId());
        output.setRoleName(role != null ? role.getRoleName() : null);
        output.setSignCode(user.getSignCode());
        output.setClientType("web");
        output.setValidPeriod(new Date(System.currentTimeMillis() + 86400000L));

        List<SysMenu> menus = sysMenuRepository.findMenusByRoleId(user.getRoleId());
        output.setRoutes(buildRouteList(menus));
        return output;
    }

    private List<Object> buildRouteList(List<SysMenu> menus) {
        List<Object> routes = new ArrayList<>();
        for (SysMenu menu : menus) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("Id", menu.getId());
            item.put("PageShowName", menu.getPageShowName());
            item.put("PageUrl", menu.getPageUrl());
            item.put("Icon", menu.getIcon() != null ? menu.getIcon() : "menu");
            item.put("LevelId", menu.getLevelId() != null ? menu.getLevelId() : 0);
            item.put("ParentPageId", menu.getParentPageId() != null ? menu.getParentPageId() : "0");
            item.put("AlwaysShow", menu.getAlwaysShow() != null ? menu.getAlwaysShow() : true);
            item.put("NoCache", menu.getNoCache() != null ? menu.getNoCache() : false);
            routes.add(item);
        }
        return routes;
    }
}

package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.PasswordChangeInput;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.TokenInput;
import com.gocent.pdjh.dto.TokenOutput;
import com.gocent.pdjh.dto.UserInput;
import com.gocent.pdjh.dto.UserPageQuery;
import com.gocent.pdjh.exception.LoginDeniedException;
import com.gocent.pdjh.exception.SessionExpiredException;
import com.gocent.pdjh.service.AdminSessionService;
import com.gocent.pdjh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/User")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminSessionService adminSessionService;

    // 登陆
    @PostMapping("/LoginUser")
    public ResultData<TokenOutput> loginUser(@RequestBody TokenInput input) {
        try {
            TokenOutput token = userService.loginUser(input);
            return ResultData.success(token, "登录成功");
        } catch (LoginDeniedException e) {
            return ResultData.fail(401, e.getMessage());
        }
    }

    // 秘钥/令牌获取等
    @PostMapping("/GetUserInfo")
    public ResultData<TokenOutput> getUserInfo(@RequestBody TokenInput input) {
        try {
            TokenOutput token = userService.getUserInfo(input);
            return ResultData.success(token, "获取用户信息成功");
        } catch (LoginDeniedException e) {
            return ResultData.fail(401, e.getMessage());
        }
    }

    // 页面刷新 数据丢失 重新获取；后台会话超过 30 分钟无操作返回 50014
    @PostMapping("/GetInfo")
    public ResultData<TokenOutput> getInfo(@RequestParam(value = "token", required = false) String token) {
        try {
            TokenOutput info = userService.getInfo(token);
            return info != null
                    ? ResultData.success(info, "获取成功")
                    : ResultData.fail(401, "Token 无效");
        } catch (SessionExpiredException e) {
            return ResultData.fail(AdminSessionService.EXPIRED_CODE, e.getMessage());
        } catch (LoginDeniedException e) {
            return ResultData.fail(401, e.getMessage());
        }
    }

    /** 后台有鼠标/键盘操作时续期；过期返回 50014。轮询不要调这个接口 */
    @PostMapping("/TouchSession")
    public ResultData<Boolean> touchSession(@RequestHeader(value = "X-Token", required = false) String token) {
        if (adminSessionService.checkToken(token, true) == AdminSessionService.Check.EXPIRED) {
            return ResultData.fail(AdminSessionService.EXPIRED_CODE, AdminSessionService.EXPIRED_MSG);
        }
        return ResultData.success(true);
    }

    // 登出
    @PostMapping("/LogOut")
    public ResultData<Boolean> logOut(@RequestParam("userno") String userno) {
        boolean ok = userService.logOut(userno);
        return ok ? ResultData.success(true, "登出成功") : ResultData.fail(500, "登出失败");
    }

    /** 分页用户列表；body 传 pageindex/pagesize/keyword/roleId/loginScene/passwordResetRequested */
    @PostMapping("/GetUsers")
    public ResultData<Map<String, Object>> getUsers(
            @RequestParam(value = "pageindex", required = false) Integer pageindexParam,
            @RequestParam(value = "pagesize", required = false) Integer pagesizeParam,
            @RequestParam(value = "keyword", required = false) String keywordParam,
            @RequestParam(value = "roleId", required = false) String roleIdParam,
            @RequestParam(value = "loginScene", required = false) String loginSceneParam,
            @RequestParam(value = "passwordResetRequested", required = false) Integer passwordResetRequestedParam,
            @RequestBody(required = false) UserPageQuery body) {
        int pageindex = pageindexParam != null ? pageindexParam : 1;
        int pagesize = pagesizeParam != null ? pagesizeParam : 20;
        String keyword = keywordParam;
        String roleId = roleIdParam;
        String loginScene = loginSceneParam;
        Integer passwordResetRequested = passwordResetRequestedParam;
        if (body != null) {
            if (body.getPageindex() != null) {
                pageindex = body.getPageindex();
            }
            if (body.getPagesize() != null) {
                pagesize = body.getPagesize();
            }
            if (body.getKeyword() != null) {
                keyword = body.getKeyword();
            }
            if (body.getRoleId() != null) {
                roleId = body.getRoleId();
            }
            if (body.getLoginScene() != null) {
                loginScene = body.getLoginScene();
            }
            if (body.getPasswordResetRequested() != null) {
                passwordResetRequested = body.getPasswordResetRequested();
            }
        }
        return ResultData.success(
                userService.getUsers(pageindex, pagesize, keyword, roleId, loginScene, passwordResetRequested),
                "获取成功");
    }

    // 新增用户
    @PostMapping("/AddUser")
    public ResultData<Boolean> addUser(@RequestBody UserInput data) {
        boolean ok = userService.addUser(data);
        return ok ? ResultData.success(true, "新增成功") : ResultData.fail(500, "新增失败（请检查帐号/角色/登录场景/手机号是否合法）");
    }

    // 修改用户信息
    @PostMapping("/EditorUser")
    public ResultData<Boolean> editorUser(@RequestBody UserInput fromUser) {
        boolean ok = userService.editorUser(fromUser);
        return ok ? ResultData.success(true, "修改成功") : ResultData.fail(500, "修改失败（请检查角色/登录场景/手机号是否合法）");
    }

    // 删除用户
    @PostMapping("/RemoveUser")
    public ResultData<Boolean> removeUser(@RequestParam("UserNo") String userNo) {
        boolean ok = userService.removeUser(userNo);
        return ok ? ResultData.success(true, "删除成功") : ResultData.fail(500, "删除失败");
    }

    @PostMapping("/GetUserClinic")
    public ResultData<Map<String, String>> getUserClinic(@RequestBody TokenInput input) {
        if (input == null || input.getUserNo() == null) {
            return ResultData.fail(500, "参数缺失");
        }
        return ResultData.success(userService.getUserClinicDept(input.getUserNo()), "获取成功");
    }

    @PostMapping("/GetPatientByCardNo")
    public ResultData<List<Map<String, String>>> getPatientByCardNo(@RequestParam("PatientCardNo") String patientCardNo) {
        return ResultData.success(userService.getPatientByCardNo(patientCardNo), "查询成功");
    }

    @PostMapping("/ResetPassWord")
    public ResultData<Boolean> resetPassWord(@RequestBody PasswordChangeInput input) {
        boolean ok = userService.resetPassword(input);
        return ok ? ResultData.success(true, "密码已更新") : ResultData.fail(500, "原密码错误或用户不存在");
    }

    /**
     * 忘记密码：登记重置申请，后台「用户管理」可见，由管理员执行 AdminResetPassword。
     * body：{ "account" | "UserNo" | "workNo": "帐号/工号或手机号" }
     * 消费者：护士站、药房工作站、后台等统一账户端
     */
    @PostMapping("/RequestPasswordReset")
    public ResultData<Boolean> requestPasswordReset(@RequestBody Map<String, Object> body) {
        try {
            String account = str(body, "account", "UserNo");
            if (account == null || account.isBlank()) {
                account = str(body, "workNo", "userNo");
            }
            userService.requestPasswordReset(account);
            return ResultData.success(true, "已提交重置申请，请联系管理员在「用户管理」中处理");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 管理员重置用户密码为默认值（123456）并强制下次登录改密。
     * 消费者：后台用户管理；body：{ "UserNo" | "userNo": "帐号" }
     */
    @PostMapping("/AdminResetPassword")
    public ResultData<Boolean> adminResetPassword(@RequestBody Map<String, Object> body) {
        try {
            String userNo = str(body, "UserNo", "userNo");
            userService.adminResetPassword(userNo);
            return ResultData.success(true, "已重置为默认密码，下次登录需修改");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    private static String str(Map<String, Object> body, String key, String alt) {
        if (body == null) {
            return "";
        }
        Object v = body.get(key);
        if (v == null && alt != null) {
            v = body.get(alt);
        }
        return v == null ? "" : v.toString().trim();
    }
}

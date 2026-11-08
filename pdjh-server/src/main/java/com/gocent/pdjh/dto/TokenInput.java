package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TokenInput {
    // 账号 — 兼容前端 "UserNo" 和 "userNo"
    @JsonProperty("UserNo")
    @JsonAlias("userNo")
    private String userNo;

    // 密码 — 兼容前端 "Password" 和 "password"
    @JsonProperty("Password")
    @JsonAlias("password")
    private String password;

    /**
     * 客户端期望场景：ADMIN=管理后台，NURSE=护士站。
     * 与 users.login_scene 比对，防止跨端登录（§23.8）。
     */
    @JsonProperty("LoginScene")
    @JsonAlias({"loginScene", "clientScene"})
    private String loginScene;
}

package com.gocent.pdjh.dto;

import lombok.Data;

@Data
public class SignDeviceInput {
    private String id;
    private String deviceCode;
    private String name;
    private String title;
    private String location;
    private String showText;
    /** 生命周期操作：DISABLE 停用 | ENABLE 解除停用；不传则由服务端按绑定推算 */
    private String lifecycleAction;
}

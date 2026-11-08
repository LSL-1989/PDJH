package com.gocent.pdjh.utils;

public class DesensitizeUtil {
    
    /**
     * 中文姓名脱敏处理
     * @param fullName 原始全名
     * @return 隐藏中间字或末字的脱敏名字
     */
    public static String desensitizeChineseName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return fullName;
        }
        String name = fullName.trim();
        int length = name.length();
        
        if (length == 2) {
            // 张三 -> 张*
            return name.substring(0, 1) + "*";
        } else if (length == 3) {
            // 王大雷 -> 王*雷
            return name.substring(0, 1) + "*" + name.substring(length - 1);
        } else if (length > 3) {
            // 司马相如 -> 司马*如 (复姓等场景简单兼容)
            return name.substring(0, 2) + "*" + name.substring(length - 1);
        }
        return name; // 只有1个字或异常保留原样
    }

    /**
     * 诊室屏展示脱敏（与 HJ候诊区大屏 pdjh2 hideInsurantName 一致，目录 HJ候诊区大屏）
     */
    public static String desensitizeForClinicDisplay(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return fullName;
        }
        String val = fullName.trim();
        if (val.length() == 2) {
            return val.substring(0, 1) + " * ";
        } else if (val.length() == 3) {
            return val.substring(0, 1) + " * " + val.substring(2, 3);
        } else if (val.length() == 4) {
            return val.substring(0, 1) + " *  * " + val.substring(3, 4);
        } else if (val.length() > 4) {
            StringBuilder name = new StringBuilder(val.substring(0, 1));
            for (int i = 0; i < val.length() - 1; i++) {
                name.append(" * ");
            }
            return name.toString();
        }
        return val;
    }
}

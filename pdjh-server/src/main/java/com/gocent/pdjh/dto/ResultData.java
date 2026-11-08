package com.gocent.pdjh.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 统一接口返回对象结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultData<T> {
    private boolean success;
    private int code;
    private String msg;
    private T data;

    public static <T> ResultData<T> success(T data) {
        return new ResultData<>(true, 200, "操作成功", data);
    }

    public static <T> ResultData<T> success(T data, String msg) {
        return new ResultData<>(true, 200, msg, data);
    }

    public static <T> ResultData<T> fail(String msg) {
        return new ResultData<>(false, 500, msg, null);
    }

    public static <T> ResultData<T> fail(int code, String msg) {
        return new ResultData<>(false, code, msg, null);
    }
}

package com.mf.fertilizer.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultVO<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> ResultVO<T> success() {
        return new ResultVO<>(200, "success", null);
    }

    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(200, "success", data);
    }

    public static <T> ResultVO<T> fail(Integer code, String msg) {
        return new ResultVO<>(code, msg, null);
    }

    public static <T> ResultVO<T> fail(String msg) {
        return new ResultVO<>(500, msg, null);
    }
}

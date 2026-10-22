package com.riskcontrol.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    DUPLICATE(409, "数据已存在"),
    SYSTEM_ERROR(500, "系统异常");

    private final int code;
    private final String message;
}

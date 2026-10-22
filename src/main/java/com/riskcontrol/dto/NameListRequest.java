package com.riskcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NameListRequest {

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "BLACK|WHITE", message = "名单类型非法")
    private String listType;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "USER_ID|ACCOUNT_NO", message = "业务维度非法")
    private String bizType;

    @NotBlank(message = "不能为空")
    @Size(max = 128, message = "长度不能超过 128")
    private String bizValue;

    @Size(max = 255, message = "长度不能超过 255")
    private String remark;

    /** 可选，默认 ENABLED */
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态非法")
    private String status;
}

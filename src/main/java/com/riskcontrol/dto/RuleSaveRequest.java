package com.riskcontrol.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RuleSaveRequest {

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "须为大写字母/数字/下划线，2~64 位")
    private String ruleCode;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String ruleName;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "LIMIT|FREQUENCY|NAMELIST|TIME_WINDOW", message = "规则类型非法")
    private String ruleType;

    @NotNull(message = "不能为空")
    @Min(value = 1, message = "必须大于等于 1")
    private Integer priority;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态非法")
    private String status;

    @NotBlank(message = "不能为空")
    @Size(max = 512, message = "长度不能超过 512")
    private String thresholdConfig;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "PASS|REVIEW|REJECT", message = "动作非法")
    private String action;
}

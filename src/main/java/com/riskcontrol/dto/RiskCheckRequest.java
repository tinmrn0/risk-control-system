package com.riskcontrol.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 交易风险检查入参 */
@Data
public class RiskCheckRequest {

    @NotBlank(message = "不能为空")
    private String transactionId;

    @NotBlank(message = "不能为空")
    private String userId;

    @NotBlank(message = "不能为空")
    private String accountNo;

    @NotNull(message = "不能为空")
    @DecimalMin(value = "0.01", message = "必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "不能为空")
    private String currency;

    @NotBlank(message = "不能为空")
    private String channel;

    @NotNull(message = "不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurTime;
}

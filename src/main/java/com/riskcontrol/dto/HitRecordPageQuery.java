package com.riskcontrol.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HitRecordPageQuery {

    @Min(value = 1, message = "必须大于等于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "必须大于等于 1")
    @Max(value = 100, message = "不能超过 100")
    private Integer pageSize = 10;

    private String userId;

    @Pattern(regexp = "PASS|REVIEW|REJECT", message = "决策值非法")
    private String decision;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}

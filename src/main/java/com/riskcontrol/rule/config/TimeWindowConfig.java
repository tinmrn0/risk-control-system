package com.riskcontrol.rule.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalTime;

/** TIME_WINDOW 规则阈值配置，对应 threshold_config JSON：{"startTime": "09:00", "endTime": "22:00"} */
@Data
public class TimeWindowConfig {

    /** 允许交易开始时间（含），不支持跨零点 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /** 允许交易结束时间（含） */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
}

package com.riskcontrol.rule.config;

import lombok.Data;

/** FREQUENCY 规则阈值配置，对应 threshold_config JSON：{"windowSeconds": 600, "maxCount": 10} */
@Data
public class FrequencyConfig {

    /** 滑动窗口长度（秒） */
    private Long windowSeconds;

    /** 窗口内允许的最大交易次数 */
    private Integer maxCount;
}

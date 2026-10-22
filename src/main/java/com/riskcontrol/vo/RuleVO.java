package com.riskcontrol.vo;

import com.riskcontrol.entity.RiskRule;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class RuleVO {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private Integer priority;
    private String status;
    private String thresholdConfig;
    private String action;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static RuleVO from(RiskRule entity) {
        RuleVO vo = new RuleVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}

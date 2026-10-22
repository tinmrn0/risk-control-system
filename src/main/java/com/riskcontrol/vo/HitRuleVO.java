package com.riskcontrol.vo;

import com.riskcontrol.rule.RuleHitResult;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HitRuleVO {

    private String ruleCode;
    private String ruleName;
    private String hitDetail;

    public static HitRuleVO from(RuleHitResult hit) {
        return new HitRuleVO(hit.getRuleCode(), hit.getRuleName(), hit.getHitDetail());
    }
}

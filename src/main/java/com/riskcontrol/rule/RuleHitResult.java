package com.riskcontrol.rule;

import com.riskcontrol.common.enums.DecisionType;
import com.riskcontrol.entity.RiskRule;
import lombok.Getter;

/** 单条规则的命中结果 */
@Getter
public class RuleHitResult {

    private final String ruleCode;
    private final String ruleName;
    private final DecisionType action;
    private final String hitDetail;

    private RuleHitResult(RiskRule rule, DecisionType action, String hitDetail) {
        this.ruleCode = rule.getRuleCode();
        this.ruleName = rule.getRuleName();
        this.action = action;
        this.hitDetail = hitDetail;
    }

    /** 动作取自规则配置的 action 字段 */
    public static RuleHitResult of(RiskRule rule, String hitDetail) {
        return new RuleHitResult(rule, DecisionType.of(rule.getAction()), hitDetail);
    }
}

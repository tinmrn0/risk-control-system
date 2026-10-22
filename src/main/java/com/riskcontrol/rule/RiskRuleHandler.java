package com.riskcontrol.rule;

import com.riskcontrol.common.enums.RuleType;
import com.riskcontrol.entity.RiskRule;

/** 规则处理器：一种规则类型对应一个实现，新增规则类型只需新增实现类 */
public interface RiskRuleHandler {

    /** 本处理器负责的规则类型 */
    RuleType supports();

    /** 执行规则：命中返回结果，未命中返回 null */
    RuleHitResult handle(RuleContext ctx, RiskRule rule);
}

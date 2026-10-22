package com.riskcontrol.service;

import com.riskcontrol.dto.RuleSaveRequest;
import com.riskcontrol.vo.PageVO;
import com.riskcontrol.vo.RuleVO;

public interface RiskRuleService {

    PageVO<RuleVO> page(long pageNum, long pageSize, String ruleType, String status);

    RuleVO create(RuleSaveRequest request);

    RuleVO update(Long id, RuleSaveRequest request);
}

package com.riskcontrol.dto;

import com.riskcontrol.vo.HitRuleVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 交易风险检查结果 */
@Data
@Builder
@AllArgsConstructor
public class RiskCheckResponse {

    private String transactionId;

    /** 最终决策：PASS / REVIEW / REJECT */
    private String decision;

    private List<HitRuleVO> hitRules;

    private Integer costMs;
}

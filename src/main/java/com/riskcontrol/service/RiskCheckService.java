package com.riskcontrol.service;

import com.riskcontrol.dto.RiskCheckRequest;
import com.riskcontrol.dto.RiskCheckResponse;

/** 交易风险检查：风控核心入口 */
public interface RiskCheckService {

    RiskCheckResponse check(RiskCheckRequest request);
}

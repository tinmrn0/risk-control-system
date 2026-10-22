package com.riskcontrol.controller;

import com.riskcontrol.common.Result;
import com.riskcontrol.dto.RiskCheckRequest;
import com.riskcontrol.dto.RiskCheckResponse;
import com.riskcontrol.service.RiskCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 交易风险检查入口 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class RiskCheckController {

    private final RiskCheckService riskCheckService;

    @PostMapping("/risk-check")
    public Result<RiskCheckResponse> riskCheck(@Valid @RequestBody RiskCheckRequest request) {
        return Result.success(riskCheckService.check(request));
    }
}

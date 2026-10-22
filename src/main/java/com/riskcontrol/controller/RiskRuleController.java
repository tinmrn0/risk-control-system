package com.riskcontrol.controller;

import com.riskcontrol.common.Result;
import com.riskcontrol.dto.RuleSaveRequest;
import com.riskcontrol.service.RiskRuleService;
import com.riskcontrol.vo.PageVO;
import com.riskcontrol.vo.RuleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RiskRuleController {

    private final RiskRuleService riskRuleService;

    @GetMapping
    public Result<PageVO<RuleVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       @RequestParam(required = false) String ruleType,
                                       @RequestParam(required = false) String status) {
        return Result.success(riskRuleService.page(pageNum, pageSize, ruleType, status));
    }

    @PostMapping
    public Result<RuleVO> create(@Valid @RequestBody RuleSaveRequest request) {
        return Result.success(riskRuleService.create(request));
    }

    @PutMapping("/{id}")
    public Result<RuleVO> update(@PathVariable Long id, @Valid @RequestBody RuleSaveRequest request) {
        return Result.success(riskRuleService.update(id, request));
    }
}

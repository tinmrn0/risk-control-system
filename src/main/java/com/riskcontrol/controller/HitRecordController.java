package com.riskcontrol.controller;

import com.riskcontrol.common.Result;
import com.riskcontrol.dto.HitRecordPageQuery;
import com.riskcontrol.service.HitRecordService;
import com.riskcontrol.vo.HitRecordVO;
import com.riskcontrol.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hit-records")
@RequiredArgsConstructor
public class HitRecordController {

    private final HitRecordService hitRecordService;

    @GetMapping
    public Result<PageVO<HitRecordVO>> page(@Valid @ModelAttribute HitRecordPageQuery query) {
        return Result.success(hitRecordService.page(query));
    }
}

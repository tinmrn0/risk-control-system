package com.riskcontrol.controller;

import com.riskcontrol.common.Result;
import com.riskcontrol.dto.NameListRequest;
import com.riskcontrol.service.NameListService;
import com.riskcontrol.vo.NameListVO;
import com.riskcontrol.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/name-lists")
@RequiredArgsConstructor
public class NameListController {

    private final NameListService nameListService;

    @GetMapping
    public Result<PageVO<NameListVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String listType,
                                           @RequestParam(required = false) String bizType) {
        return Result.success(nameListService.page(pageNum, pageSize, listType, bizType));
    }

    @PostMapping
    public Result<NameListVO> create(@Valid @RequestBody NameListRequest request) {
        return Result.success(nameListService.create(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        nameListService.delete(id);
        return Result.success(null);
    }
}

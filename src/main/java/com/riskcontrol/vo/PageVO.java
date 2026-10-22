package com.riskcontrol.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageVO<T> {

    private long total;
    private List<T> records;

    public static <T> PageVO<T> of(IPage<T> page) {
        return new PageVO<>(page.getTotal(), page.getRecords());
    }
}

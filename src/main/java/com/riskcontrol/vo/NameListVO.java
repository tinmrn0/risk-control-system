package com.riskcontrol.vo;

import com.riskcontrol.entity.RiskNameList;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class NameListVO {

    private Long id;
    private String listType;
    private String bizType;
    private String bizValue;
    private String remark;
    private String status;
    private LocalDateTime createTime;

    public static NameListVO from(RiskNameList entity) {
        NameListVO vo = new NameListVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}

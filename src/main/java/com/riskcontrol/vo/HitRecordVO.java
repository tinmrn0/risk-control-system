package com.riskcontrol.vo;

import com.riskcontrol.entity.RiskHitRecord;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class HitRecordVO {

    private Long id;
    private String transactionId;
    private String userId;
    private String ruleCode;
    private String decision;
    private String hitDetail;
    private Integer costMs;
    private LocalDateTime createTime;

    public static HitRecordVO from(RiskHitRecord entity) {
        HitRecordVO vo = new HitRecordVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}

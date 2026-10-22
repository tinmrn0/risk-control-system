package com.riskcontrol.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_hit_record")
public class RiskHitRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transactionId;

    private String userId;

    private String ruleCode;

    private String decision;

    private String hitDetail;

    private Integer costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.hxy.domain.trade.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeLockRuleCommandEntity {

    private String userId;
    private Long activityId;

    /** 组队ID；为空表示首次开团 */
    private String teamId;
}

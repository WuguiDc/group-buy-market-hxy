package com.hxy.domain.trade.model.entity;

import lombok.*;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundBehaviorEntity {

    private String userId;

    private String orderId;

    private String teamId;

    private TradeRefundBehaviorEnum tradeRefundBehaviorEnum;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public enum TradeRefundBehaviorEnum {
        SUCCESS("success","成功"),
        REPEAT("repeat","重复"),
        FAIL("fail","失败"),
        ;


        private String code;
        private String info;
    }

}

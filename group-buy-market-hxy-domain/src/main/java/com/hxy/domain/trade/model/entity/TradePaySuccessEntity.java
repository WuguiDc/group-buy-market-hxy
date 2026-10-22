package com.hxy.domain.trade.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradePaySuccessEntity {

    private String source;
    private String channel;
    private String userId;
    private String outTradeNo;
    /** 外部交易时间 */
    private Date outTradeTime;
}

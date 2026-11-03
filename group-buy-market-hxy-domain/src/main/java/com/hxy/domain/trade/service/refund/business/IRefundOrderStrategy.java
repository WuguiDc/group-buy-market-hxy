package com.hxy.domain.trade.service.refund.business;

import com.hxy.domain.trade.model.entity.TradeRefundOrderEntity;

public interface IRefundOrderStrategy {

    void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity);
}

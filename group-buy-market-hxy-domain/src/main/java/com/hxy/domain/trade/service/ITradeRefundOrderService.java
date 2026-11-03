package com.hxy.domain.trade.service;

import com.hxy.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.hxy.domain.trade.model.entity.TradeRefundCommandEntity;

public interface ITradeRefundOrderService {

    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity);
}

package com.hxy.domain.trade.service;

import com.hxy.domain.trade.model.entity.TradePaySettlementEntity;
import com.hxy.domain.trade.model.entity.TradePaySuccessEntity;

public interface ITradeSettlementOrderService {

    TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception;


}

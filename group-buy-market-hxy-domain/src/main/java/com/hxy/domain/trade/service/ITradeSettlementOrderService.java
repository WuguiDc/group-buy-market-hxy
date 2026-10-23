package com.hxy.domain.trade.service;

import com.hxy.domain.trade.model.entity.TradePaySettlementEntity;
import com.hxy.domain.trade.model.entity.TradePaySuccessEntity;

import java.util.Map;

public interface ITradeSettlementOrderService {

    TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception;

    Map<String,Integer> execSettlementNotifyJob() throws Exception;

    Map<String,Integer> execSettlementNotifyJob(String teamId) throws Exception;

}

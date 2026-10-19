package com.hxy.domain.trade.adapter.repository;

import com.hxy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hxy.domain.trade.model.entity.MarketPayOrderEntity;
import com.hxy.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeRepository {
    public MarketPayOrderEntity queryMarketPayOrderByOutTradeNo(String userId, String outTradeNo);

    public GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);
}

package com.hxy.domain.trade.service;

import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hxy.domain.trade.model.entity.MarketPayOrderEntity;
import com.hxy.domain.trade.model.entity.PayActivityEntity;
import com.hxy.domain.trade.model.entity.PayDiscountEntity;
import com.hxy.domain.trade.model.entity.UserEntity;
import com.hxy.domain.trade.model.valobj.GroupBuyProgressVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TradeOrderService implements ITradeOrderService {

    @Resource
    private ITradeRepository repository;

    @Override
    public MarketPayOrderEntity queryMarketPayOrderByOutTradeNo(String userId,String outTradeNo) {
        return repository.queryMarketPayOrderByOutTradeNo(userId,outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        return repository.queryGroupBuyProgress(teamId);
    }

    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity,  PayActivityEntity payActivityEntity,PayDiscountEntity payDiscountEntity) {
        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .build();

        return repository.lockMarketPayOrder(groupBuyOrderAggregate);
    }
}

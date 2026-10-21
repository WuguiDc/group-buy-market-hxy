package com.hxy.domain.trade.service;

import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hxy.domain.trade.model.entity.*;
import com.hxy.domain.trade.model.valobj.GroupBuyProgressVO;
import com.hxy.domain.trade.service.factory.TradeRuleFilterFactory;
import com.hxy.types.design.framework.link.model2.chain.BusinessLinkedList;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TradeOrderService implements ITradeOrderService {

    @Resource
    private ITradeRepository repository;

    @Resource
    private BusinessLinkedList<TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> tradeRuleFilter;

    @Override
    public MarketPayOrderEntity queryMarketPayOrderByOutTradeNo(String userId,String outTradeNo) {
        return repository.queryMarketPayOrderByOutTradeNo(userId,outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        return repository.queryGroupBuyProgress(teamId);
    }


    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity,  PayActivityEntity payActivityEntity,PayDiscountEntity payDiscountEntity) throws Exception {
        //过滤
        TradeRuleFilterBackEntity tradeRuleFilterBackEntity = tradeRuleFilter.apply(TradeRuleCommandEntity
                        .builder()
                        .activityId(payActivityEntity.getActivityId())
                        .userId(userEntity.getUserId())
                        .build(),
                new TradeRuleFilterFactory.DynamicContext()
        );
        Integer userTakeOrderCount = tradeRuleFilterBackEntity.getUserTakeOrderCount();

        //构造聚合对象
        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .userTakeOrderCount(userTakeOrderCount)
                .build();
        //锁定聚合对象
        return repository.lockMarketPayOrder(groupBuyOrderAggregate);
    }
}

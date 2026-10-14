package com.hxy.domain.activity.service.trail.node;


import com.hxy.domain.activity.model.entity.MarketProductEntity;
import com.hxy.domain.activity.model.entity.TrialBalanceEntity;
import com.hxy.domain.activity.service.trail.AbstractGroupBuyMarketSupport;
import com.hxy.domain.activity.service.trail.factory.DefaultActivityStrategyFactory;
import com.hxy.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class SwitchNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private MarketNode marketNode;

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return router(requestParameter,dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return marketNode;
    }
}

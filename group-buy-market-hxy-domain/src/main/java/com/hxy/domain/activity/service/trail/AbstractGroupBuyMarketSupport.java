package com.hxy.domain.activity.service.trail;

import com.hxy.domain.activity.adapter.repository.IActivityRepository;
import com.hxy.types.design.framework.tree.AbstractMultiThreadStrategyRouter;
import com.hxy.types.design.framework.tree.AbstractStrategyRouter;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity,DynamicContext,TrialBalanceEntity> extends AbstractMultiThreadStrategyRouter<MarketProductEntity,DynamicContext,TrialBalanceEntity> {

    protected long timeout = 500;

    @Resource
    protected IActivityRepository repository;

    @Override
    protected void mutiThread(MarketProductEntity requestParameter, DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }
}

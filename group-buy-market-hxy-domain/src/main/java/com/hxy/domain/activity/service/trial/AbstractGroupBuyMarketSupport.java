package com.hxy.domain.activity.service.trial;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import com.hxy.domain.activity.adapter.repository.IActivityRepository;
import com.hxy.domain.activity.model.entity.MarketProductEntity;
import com.hxy.domain.activity.model.entity.TrialBalanceEntity;
import com.hxy.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;


import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity,DynamicContext,TrialBalanceEntity> extends AbstractMultiThreadStrategyRouter<
        com.hxy.domain.activity.model.entity.MarketProductEntity,
        DefaultActivityStrategyFactory.DynamicContext,
        com.hxy.domain.activity.model.entity.TrialBalanceEntity> {

    protected long timeout = 5000;

    @Resource
    protected IActivityRepository repository;



    @Override
    protected void multiThread(com.hxy.domain.activity.model.entity.MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 缺省的方法
    }


}

package com.hxy.domain.activity.service.trail.node;

import com.hxy.domain.activity.model.entity.MarketProductEntity;
import com.hxy.domain.activity.model.entity.TrialBalanceEntity;
import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.model.valobj.SkuVO;
import com.hxy.domain.activity.service.trail.AbstractGroupBuyMarketSupport;
import com.hxy.domain.activity.service.trail.factory.DefaultActivityStrategyFactory;
import com.hxy.domain.activity.service.trail.factory.thread.QueryGroupBuyActivityDiscountVOThreadTask;
import com.hxy.domain.activity.service.trail.factory.thread.QuerySkuVOFromDBThreadTask;
import com.hxy.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.*;

@Slf4j
@Service
public class MarketNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private EndNode endNode;

    @Override
    protected void mutiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        QueryGroupBuyActivityDiscountVOThreadTask queryGroupBuyActivityDiscountVOThreadTask = new QueryGroupBuyActivityDiscountVOThreadTask(requestParameter.getSource(), requestParameter.getChannel(), repository);
        FutureTask<GroupBuyActivityDiscountVO> QueryGroupBuyActivityDiscountVOFutureTask = new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);
        threadPoolExecutor.submit(QueryGroupBuyActivityDiscountVOFutureTask);

        QuerySkuVOFromDBThreadTask querySkuVOFromDBThreadTask = new QuerySkuVOFromDBThreadTask(requestParameter.getGoodsId(), repository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(querySkuVOFromDBThreadTask);
        threadPoolExecutor.submit(skuVOFutureTask);

        dynamicContext.setGroupBuyActivityDiscountVO(QueryGroupBuyActivityDiscountVOFutureTask.get(timeout, TimeUnit.MINUTES));
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MINUTES));
    }

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return router(requestParameter,dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return endNode;
    }
}

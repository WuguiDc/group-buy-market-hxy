package com.hxy.domain.activity.service.trail.factory;

import com.hxy.domain.activity.model.entity.MarketProductEntity;
import com.hxy.domain.activity.model.entity.TrialBalanceEntity;
import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.model.valobj.SkuVO;
import com.hxy.domain.activity.service.trail.node.RootNode;
import com.hxy.types.design.framework.tree.StrategyHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class DefaultActivityStrategyFactory {

    private final RootNode rootNode;

    public  DefaultActivityStrategyFactory(RootNode rootNode) {
        this.rootNode = rootNode;
    }

    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler() {
        return rootNode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext{

        private GroupBuyActivityDiscountVO groupBuyActivityDiscountVO;
        private SkuVO skuVO;

    }
}

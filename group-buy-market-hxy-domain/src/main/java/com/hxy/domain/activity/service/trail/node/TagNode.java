package com.hxy.domain.activity.service.trail.node;

import com.hxy.domain.activity.model.entity.MarketProductEntity;
import com.hxy.domain.activity.model.entity.TrialBalanceEntity;
import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.service.trail.AbstractGroupBuyMarketSupport;
import com.hxy.domain.activity.service.trail.factory.DefaultActivityStrategyFactory;
import com.hxy.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class TagNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {




    @Resource
    private EndNode endNode;


    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        //获取拼团活动配置
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();

        String tagId = groupBuyActivityDiscountVO.getTagId();
        boolean visible = groupBuyActivityDiscountVO.isVisible();
        boolean enable = groupBuyActivityDiscountVO.isEnable();

        if(StringUtils.isBlank(tagId)){
            dynamicContext.setVisible(true);
            dynamicContext.setEnable(true);
            return router(requestParameter,dynamicContext);
        }

        //是否在人群标签范围内
        boolean isWithin =repository.isTagCrowdRange(tagId, requestParameter.getUserId());
        dynamicContext.setVisible(isWithin || visible);
        dynamicContext.setEnable(isWithin || enable);

        return router(requestParameter,dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return endNode;
    }
}

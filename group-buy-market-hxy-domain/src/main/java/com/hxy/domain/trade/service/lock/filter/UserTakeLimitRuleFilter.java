package com.hxy.domain.trade.service.lock.filter;

import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hxy.domain.trade.model.entity.TradeRuleCommandEntity;
import com.hxy.domain.trade.model.entity.TradeRuleFilterBackEntity;
import com.hxy.domain.trade.service.lock.factory.TradeRuleFilterFactory;
import com.hxy.types.design.framework.link.model2.handler.ILogicHandler;
import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeRuleFilterBackEntity apply(TradeRuleCommandEntity requestParameter, TradeRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-用户参与次数校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());
        GroupBuyActivityEntity  groupBuyActivityEntity =dynamicContext.getGroupBuyActivityEntity();
        Integer count = repository.queryOrderCountByActivityId(requestParameter.getUserId(),requestParameter.getActivityId());

        if (null != groupBuyActivityEntity && count >= groupBuyActivityEntity.getTakeLimitCount()) {
            throw new AppException(ResponseCode.E0103);
        }


        return TradeRuleFilterBackEntity.builder().userTakeOrderCount(count).build();
    }
}

package com.hxy.domain.trade.service.lock.filter;

import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hxy.domain.trade.model.entity.TradeRuleCommandEntity;
import com.hxy.domain.trade.model.entity.TradeRuleFilterBackEntity;
import com.hxy.domain.trade.service.lock.factory.TradeRuleFilterFactory;
import com.hxy.types.design.framework.link.model2.handler.ILogicHandler;
import com.hxy.types.enums.ActivityStatusEnumVO;
import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;


@Slf4j
@Service
public class ActivityUsabilityRuleFilter implements ILogicHandler<TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeRuleFilterBackEntity apply(TradeRuleCommandEntity requestParameter, TradeRuleFilterFactory.DynamicContext dynamicContext) throws Exception {

        log.info("交易规则过滤-活动的可用性校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        GroupBuyActivityEntity groupBuyActivityEntity = repository.queryGroupBuyActivityByActivityId(requestParameter.getActivityId());

        if (!ActivityStatusEnumVO.EFFECTIVE.equals(groupBuyActivityEntity.getStatus())) {
            throw new AppException(ResponseCode.E0101);
        }

        Date currentTime = new Date();
        if (currentTime.before(groupBuyActivityEntity.getStartTime()) || currentTime.after(groupBuyActivityEntity.getEndTime())) {
            throw new  AppException(ResponseCode.E0102);
        }

        dynamicContext.setGroupBuyActivityEntity(groupBuyActivityEntity);

        return next(requestParameter,dynamicContext);
    }
}

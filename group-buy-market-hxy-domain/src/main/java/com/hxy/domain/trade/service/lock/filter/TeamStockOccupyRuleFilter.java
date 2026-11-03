package com.hxy.domain.trade.service.lock.filter;

import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hxy.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.hxy.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.hxy.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/** 2-19：组队名额预占规则。 */
@Slf4j
@Service
public class TeamStockOccupyRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-组队库存校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());
        String teamId = requestParameter.getTeamId();
        // 首次开团已占一个名额，此处只限制加入已有队伍的请求。
        if (StringUtils.isBlank(teamId)) {
            return TradeLockRuleFilterBackEntity.builder()
                    .userTakeOrderCount(dynamicContext.getUserTakeOrderCount())
                    .build();
        }

        GroupBuyActivityEntity activity = dynamicContext.getGroupBuyActivityEntity();
        String teamStockKey = dynamicContext.generateTeamStockKey(teamId);
        String recoveryTeamStockKey = dynamicContext.generateRecoveryTeamStockKey(teamId);
        boolean occupied = repository.occupyTeamStock(teamStockKey, recoveryTeamStockKey, activity.getTarget(), activity.getValidTime());
        if (!occupied) {
            log.warn("交易规则过滤-组队库存校验{} activityId:{} 抢占失败:{}", requestParameter.getUserId(), requestParameter.getActivityId(), teamStockKey);
            throw new AppException(ResponseCode.E0008);
        }
        return TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(dynamicContext.getUserTakeOrderCount())
                .recoveryTeamStockKey(recoveryTeamStockKey)
                .build();
    }
}

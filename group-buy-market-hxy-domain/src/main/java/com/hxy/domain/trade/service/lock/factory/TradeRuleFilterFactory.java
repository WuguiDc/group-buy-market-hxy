package com.hxy.domain.trade.service.lock.factory;


import com.hxy.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hxy.domain.trade.model.entity.TradeRuleCommandEntity;
import com.hxy.domain.trade.model.entity.TradeRuleFilterBackEntity;
import com.hxy.domain.trade.service.lock.filter.ActivityUsabilityRuleFilter;
import com.hxy.domain.trade.service.lock.filter.UserTakeLimitRuleFilter;
import com.hxy.types.design.framework.link.model2.LinkArmory;
import com.hxy.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TradeRuleFilterFactory {

    @Bean("tradeRuleFilter")
    public BusinessLinkedList <TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> tradeRuleFilter(ActivityUsabilityRuleFilter activityUsabilityRuleFilter, UserTakeLimitRuleFilter userTakeLimitRuleFilter){
        LinkArmory<TradeRuleCommandEntity, DynamicContext, TradeRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易规则过滤链", activityUsabilityRuleFilter, userTakeLimitRuleFilter);

        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext{
        private GroupBuyActivityEntity groupBuyActivityEntity;

    }
}

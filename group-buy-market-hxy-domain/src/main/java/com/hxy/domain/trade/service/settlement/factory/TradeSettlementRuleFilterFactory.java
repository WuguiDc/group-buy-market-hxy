package com.hxy.domain.trade.service.settlement.factory;

import com.hxy.domain.trade.model.entity.*;
import com.hxy.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hxy.domain.trade.service.settlement.filter.EndRuleFilter;
import com.hxy.domain.trade.service.settlement.filter.OutTradeNoRuleFilter;
import com.hxy.domain.trade.service.settlement.filter.SCRuleFilter;
import com.hxy.domain.trade.service.settlement.filter.SettableRuleFilter;
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
public class TradeSettlementRuleFilterFactory {

    @Bean("tradeSettlementRuleFilter")
    public BusinessLinkedList<TradeSettlementRuleCommandEntity,
            TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter(
               SCRuleFilter scRuleFilter,
               OutTradeNoRuleFilter outTradeNoRuleFilter,
               SettableRuleFilter settableRuleFilter,
               EndRuleFilter endRuleFilter
    ){
        LinkArmory<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易规则过滤链", scRuleFilter, outTradeNoRuleFilter,settableRuleFilter,endRuleFilter);

        return linkArmory.getLogicLink();

    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext{
        private MarketPayOrderEntity marketPayOrderEntity;
        private GroupBuyTeamEntity groupBuyTeamEntity;

    }
}

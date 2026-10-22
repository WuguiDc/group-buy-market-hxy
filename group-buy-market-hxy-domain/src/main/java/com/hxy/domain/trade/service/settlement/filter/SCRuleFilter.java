package com.hxy.domain.trade.service.settlement.filter;


import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hxy.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hxy.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hxy.types.design.framework.link.model2.handler.ILogicHandler;
import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class SCRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {

        boolean Intercept=repository.isSCBlackIntercepter(requestParameter.getSource(),requestParameter.getChannel());
        if(Intercept){
            throw new AppException(ResponseCode.E0105);
        }

        return next(requestParameter,dynamicContext);
    }
}

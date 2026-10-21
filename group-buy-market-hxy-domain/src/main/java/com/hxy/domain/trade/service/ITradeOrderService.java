package com.hxy.domain.trade.service;

import com.hxy.domain.trade.model.entity.MarketPayOrderEntity;
import com.hxy.domain.trade.model.entity.PayActivityEntity;
import com.hxy.domain.trade.model.entity.PayDiscountEntity;
import com.hxy.domain.trade.model.entity.UserEntity;
import com.hxy.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeOrderService {



    MarketPayOrderEntity queryMarketPayOrderByOutTradeNo(String userId,String outTradeNo);

    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity,PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception;


}

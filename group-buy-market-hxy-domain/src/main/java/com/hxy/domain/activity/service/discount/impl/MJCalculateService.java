package com.hxy.domain.activity.service.discount.impl;

import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.service.discount.AbstractDiscountCalculateService;
import com.hxy.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service("MJ")
@Slf4j
public class MJCalculateService extends AbstractDiscountCalculateService {
    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {

        String marketExpr = groupBuyDiscount.getMarketExpr();;

        String[] split = marketExpr.split(Constants.SPLIT);
        BigDecimal x =  new BigDecimal(split[0]);
        BigDecimal y =  new BigDecimal(split[1]);

        //不满足满减的价格就直接返回
        if(originalPrice.compareTo(x)<0){
            return originalPrice;
        }
        BigDecimal deducionPrice = originalPrice.subtract(y);
        //折扣价格打完折小于等于0元
        if(deducionPrice.compareTo(BigDecimal.ZERO)<=0){
            return new BigDecimal("0.01");
        }

        return deducionPrice;
    }
}

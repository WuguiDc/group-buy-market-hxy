package com.hxy.domain.activity.service.discount.impl;

import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service("ZK")
public class ZKCalculateService extends AbstractDiscountCalculateService {
    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {

        String marketExpr = groupBuyDiscount.getMarketExpr();

        BigDecimal deducionPrice = originalPrice.multiply(new BigDecimal(marketExpr));

        if(deducionPrice.compareTo(BigDecimal.ZERO)<=0){
            return new BigDecimal("0.01");
        }

        return deducionPrice;
    }
}

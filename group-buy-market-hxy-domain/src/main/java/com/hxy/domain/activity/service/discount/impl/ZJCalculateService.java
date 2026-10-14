package com.hxy.domain.activity.service.discount.impl;

import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.service.discount.AbstractDiscountCalculateService;
import com.hxy.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

@Slf4j
@Service("ZJ")
public class ZJCalculateService extends AbstractDiscountCalculateService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {

        String marketExpr = groupBuyDiscount.getMarketExpr();;


        BigDecimal deductionPrice = originalPrice.subtract(new  BigDecimal(marketExpr));

        return deductionPrice;
    }
}

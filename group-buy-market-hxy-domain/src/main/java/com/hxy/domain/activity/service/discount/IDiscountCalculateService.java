package com.hxy.domain.activity.service.discount;

import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;

public interface IDiscountCalculateService {

    BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

}

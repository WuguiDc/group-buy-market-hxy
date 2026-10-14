package com.hxy.domain.activity.service.discount;

import com.hxy.domain.activity.model.valobj.DiscountTypeEnum;
import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;

public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService {
    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        if (DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())) {
            boolean isCrowdRange= filterTagId(userId,groupBuyDiscount.getTagId());
            if (isCrowdRange) return originalPrice;

        }

        return doCalculate(originalPrice,groupBuyDiscount);
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

    private boolean filterTagId(String userId, String tagId) {
        return true;
    }


}

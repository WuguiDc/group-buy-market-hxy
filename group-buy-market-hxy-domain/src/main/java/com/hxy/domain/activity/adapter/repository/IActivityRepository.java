package com.hxy.domain.activity.adapter.repository;

import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.model.valobj.SkuVO;

public interface IActivityRepository {

    GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(String source,String channel);

    SkuVO querySkuByGoodsId(String goodsId);
}

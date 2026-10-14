package com.hxy.domain.activity.service.trail.factory.thread;

import com.hxy.domain.activity.adapter.repository.IActivityRepository;
import com.hxy.domain.activity.model.valobj.SkuVO;

import java.util.concurrent.Callable;

public class QuerySkuVOFromDBThreadTask implements Callable<SkuVO> {

    private final String goodsId;

    private final IActivityRepository activityRepository;

    public QuerySkuVOFromDBThreadTask(String goodsId,IActivityRepository activityRepository) {
        this.goodsId = goodsId;
        this.activityRepository = activityRepository;
    }


    @Override
    public SkuVO call() throws Exception {
        return activityRepository.querySkuByGoodsId(goodsId);
    }
}

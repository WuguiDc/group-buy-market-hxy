package com.hxy.domain.activity.service.trial.factory.thread;

import com.hxy.domain.activity.adapter.repository.IActivityRepository;
import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.model.valobj.SCSkuActivityVO;

import java.util.concurrent.Callable;

public class QueryGroupBuyActivityDiscountVOThreadTask implements Callable<GroupBuyActivityDiscountVO> {

    private final String source;
    private final String channel;

    private final String goodsId;

    private final IActivityRepository repository;
    public QueryGroupBuyActivityDiscountVOThreadTask(String source, String channel, String goodsId, IActivityRepository repository) {
        this.source = source;
        this.channel = channel;
        this.goodsId = goodsId;
        this.repository = repository;
    }


    @Override
    public GroupBuyActivityDiscountVO call() throws Exception {
        SCSkuActivityVO scSkuActivityVO = repository.querySCSkuActivityBySCGoodsId(source,channel,goodsId);
        if (scSkuActivityVO==null) return  null;
        return repository.queryGroupBuyActivityDiscountVO(scSkuActivityVO.getActivityId());
    }
}

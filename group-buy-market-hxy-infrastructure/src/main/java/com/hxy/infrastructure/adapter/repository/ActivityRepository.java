package com.hxy.infrastructure.adapter.repository;

import com.hxy.domain.activity.adapter.repository.IActivityRepository;
import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.model.valobj.SkuVO;
import com.hxy.infrastructure.dao.IGroupBuyActivityDao;
import com.hxy.infrastructure.dao.IGroupBuyDiscountDao;
import com.hxy.infrastructure.dao.ISkuDao;
import com.hxy.infrastructure.dao.po.GroupBuyActivity;
import com.hxy.infrastructure.dao.po.GroupBuyDiscount;
import com.hxy.infrastructure.dao.po.Sku;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class ActivityRepository implements IActivityRepository {

    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    @Resource
    private IGroupBuyDiscountDao groupBuyDiscountDao;

    @Resource
    private ISkuDao skuDao;

    @Override
    public GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(String source, String channel) {
        // 根据SC渠道值查询配置中最新的1个有效的活动
        GroupBuyActivity groupBuyActivityReq = new GroupBuyActivity();
        groupBuyActivityReq.setSource(source);
        groupBuyActivityReq.setChannel(channel);
        GroupBuyActivity groupBuyActivityRes = groupBuyActivityDao.queryValidGroupBuyActivity(groupBuyActivityReq);

        String discountId = groupBuyActivityRes.getDiscountId();

        GroupBuyDiscount groupBuyDiscountRes = groupBuyDiscountDao.queryGroupBuyActivityDiscountByDiscountId(discountId);
        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountName(groupBuyDiscountRes.getDiscountName())
                .discountDesc(groupBuyDiscountRes.getDiscountDesc())
                .discountType(groupBuyDiscountRes.getDiscountType())
                .marketPlan(groupBuyDiscountRes.getMarketPlan())
                .marketExpr(groupBuyDiscountRes.getMarketExpr())
                .tagId(groupBuyDiscountRes.getTagId())
                .build();

        return GroupBuyActivityDiscountVO.builder()
                .activityId(groupBuyActivityRes.getActivityId())
                .activityName(groupBuyActivityRes.getActivityName())
                .source(groupBuyActivityRes.getSource())
                .channel(groupBuyActivityRes.getChannel())
                .goodsId(groupBuyActivityRes.getGoodsId())
                .groupBuyDiscount(groupBuyDiscount)
                .groupType(groupBuyActivityRes.getGroupType())
                .takeLimitCount(groupBuyActivityRes.getTakeLimitCount())
                .target(groupBuyActivityRes.getTarget())
                .validTime(groupBuyActivityRes.getValidTime())
                .status(groupBuyActivityRes.getStatus())
                .startTime(groupBuyActivityRes.getStartTime())
                .endTime(groupBuyActivityRes.getEndTime())
                .tagId(groupBuyActivityRes.getTagId())
                .tagScope(groupBuyActivityRes.getTagScope())
                .build();
    }

    @Override
    public SkuVO querySkuByGoodsId(String goodsId) {
        Sku sku = skuDao.querySkuByGoodsId(goodsId);
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

}

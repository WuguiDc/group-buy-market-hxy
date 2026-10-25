package com.hxy.domain.activity.service;


import com.hxy.domain.activity.adapter.repository.IActivityRepository;
import com.hxy.domain.activity.model.entity.MarketProductEntity;
import com.hxy.domain.activity.model.entity.TrialBalanceEntity;
import com.hxy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hxy.domain.activity.model.valobj.TeamStatisticVO;
import com.hxy.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hxy.types.design.framework.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 首页营销服务
 * @create 2024-12-14 14:33
 */
@Service
public class IndexGroupBuyMarketServiceImpl implements IIndexGroupBuyMarketService {

    @Resource
    private DefaultActivityStrategyFactory defaultActivityStrategyFactory;
    @Resource
    private IActivityRepository activityRepository;

    @Override
    public TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception {
        //获取执行策略
        StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler = defaultActivityStrategyFactory.strategyHandler();
        //受理试算操作
        TrialBalanceEntity trialBalanceEntity = strategyHandler.apply(marketProductEntity, new DefaultActivityStrategyFactory.DynamicContext());

        return trialBalanceEntity;
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailList(Long activityId, String userId, Integer ownerCount, Integer randomCount) {
        List<UserGroupBuyOrderDetailEntity> uninAllList =  new ArrayList<>();

        if(0!=ownerCount){
            List<UserGroupBuyOrderDetailEntity> ownerList= activityRepository.queryInProgressUserOrderBuyDetailListByOwner(activityId,userId,ownerCount);
            if (null != ownerList && !ownerList.isEmpty()) {
                uninAllList.addAll(ownerList);
            }
        }

        if(0!=randomCount){
            List<UserGroupBuyOrderDetailEntity> randomList= activityRepository.queryInProgressUserOrderBuyDetailListByRandom(activityId,userId,randomCount);
            if (null != randomList && !randomList.isEmpty()) {
                uninAllList.addAll(randomList);
            }
        }

        return uninAllList;
    }

    @Override
    public TeamStatisticVO queryTeamStatisticByActivityId(Long activityId) throws Exception {
        return activityRepository.queryTeamStatisticByActivityId(activityId);
    }

}

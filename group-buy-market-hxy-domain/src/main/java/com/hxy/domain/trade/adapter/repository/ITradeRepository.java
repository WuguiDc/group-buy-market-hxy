package com.hxy.domain.trade.adapter.repository;

import com.hxy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hxy.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hxy.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hxy.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hxy.domain.trade.model.entity.MarketPayOrderEntity;
import com.hxy.domain.trade.model.entity.NotifyTaskEntity;
import com.hxy.domain.trade.model.valobj.GroupBuyProgressVO;

import java.util.List;

public interface ITradeRepository {
    public MarketPayOrderEntity queryMarketPayOrderByOutTradeNo(String userId, String outTradeNo);

    public GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);


    GroupBuyActivityEntity queryGroupBuyActivityByActivityId(Long activityId);

    Integer queryOrderCountByActivityId(String userId, Long activityId);

    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);

    boolean settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

    boolean isSCBlackIntercepter(String source, String channel);

    List<NotifyTaskEntity> queryUnExecuteNotifyTaskList();

    List<NotifyTaskEntity> queryUnExecuteNotifyTaskList(String teamId);

    int updateNotifyStatusSuccess(String teamId);

    int updateNotifyStatusError(String teamId);

    int updateNotifyStatusRetry(String teamId);
}

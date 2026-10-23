package com.hxy.infrastructure.adapter.repository;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hxy.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hxy.domain.trade.model.entity.*;
import com.hxy.domain.trade.model.valobj.GroupBuyProgressVO;
import com.hxy.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hxy.infrastructure.dao.IGroupBuyActivityDao;
import com.hxy.infrastructure.dao.IGroupBuyOrderDao;
import com.hxy.infrastructure.dao.IGroupBuyOrderListDao;
import com.hxy.infrastructure.dao.INotifyTaskDao;
import com.hxy.infrastructure.dao.po.GroupBuyActivity;
import com.hxy.infrastructure.dao.po.GroupBuyOrder;
import com.hxy.infrastructure.dao.po.GroupBuyOrderList;
import com.hxy.infrastructure.dao.po.NotifyTask;
import com.hxy.infrastructure.dcc.DccService;
import com.hxy.types.common.Constants;
import com.hxy.types.enums.ActivityStatusEnumVO;
import com.hxy.types.enums.GroupBuyOrderEnumVO;
import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;


@Repository
public class TradeRepository implements ITradeRepository {
    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    @Resource
    private INotifyTaskDao notifyTaskDao;
    @Resource
    private DccService dccService;


    @Override
    public MarketPayOrderEntity queryMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {

        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setOutTradeNo(outTradeNo);
        groupBuyOrderListReq.setUserId(userId);


        GroupBuyOrderList groupBuyOrderListRes = groupBuyOrderListDao.queryGroupBuyOrderRecordByOutTradeNo(groupBuyOrderListReq);

        if (null == groupBuyOrderListRes) return  null;

        return MarketPayOrderEntity.builder()
                .teamId(groupBuyOrderListRes.getTeamId())
                .orderId(groupBuyOrderListRes.getOrderId())
                .deductionPrice(groupBuyOrderListRes.getDeductionPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.valueOf(groupBuyOrderListRes.getStatus()))
                .build();
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        GroupBuyOrder groupBuyOrder = groupBuyOrderDao.queryGroupBuyProgress(teamId);

        if (null == groupBuyOrder) return  null;

        return GroupBuyProgressVO.builder()
                .targetCount(groupBuyOrder.getTargetCount())
                .completeCount(groupBuyOrder.getCompleteCount())
                .lockCount(groupBuyOrder.getLockCount())
                .build();
    }


    @Transactional(timeout = 500)
    @Override
    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate) {
        // 聚合对象信息
        UserEntity userEntity = groupBuyOrderAggregate.getUserEntity();
        PayActivityEntity payActivityEntity = groupBuyOrderAggregate.getPayActivityEntity();
        PayDiscountEntity payDiscountEntity = groupBuyOrderAggregate.getPayDiscountEntity();
        Integer userTakeOrderCount = groupBuyOrderAggregate.getUserTakeOrderCount();

        String teamId = payActivityEntity.getTeamId();

        if(StringUtils.isBlank(teamId)){
            teamId = RandomStringUtils.randomNumeric(8);

            Date currentDate = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.MINUTE, payActivityEntity.getValidTime());

            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .teamId(teamId)
                    .activityId(payActivityEntity.getActivityId())
                    .source(payDiscountEntity.getSource())
                    .channel(payDiscountEntity.getChannel())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .payPrice(payDiscountEntity.getPayPrice())
                    .targetCount(payActivityEntity.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .validStartTime(currentDate)
                    .validEndTime(calendar.getTime())
                    .notifyUrl(payDiscountEntity.getNotifyUrl())
                    .build();

            groupBuyOrderDao.insert(groupBuyOrder);
        }else {
            int updateAddLockCount = groupBuyOrderDao.updateAddLockCount(teamId);
            if (1!=updateAddLockCount){
                throw new AppException(ResponseCode.E0005);
            }
        }

        String orderId = RandomStringUtils.randomNumeric(12);

        GroupBuyOrderList groupBuyOrderListReq = GroupBuyOrderList.builder()
                .userId(userEntity.getUserId())
                .teamId(teamId)
                .orderId(orderId)
                .activityId(payActivityEntity.getActivityId())
                .startTime(payActivityEntity.getStartTime())
                .endTime(payActivityEntity.getEndTime())
                .goodsId(payDiscountEntity.getGoodsId())
                .source(payDiscountEntity.getSource())
                .channel(payDiscountEntity.getChannel())
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo())
                .bizId(payActivityEntity.getActivityId()+ Constants.UNDERLINE +userEntity.getUserId() +Constants.UNDERLINE + (userTakeOrderCount+1))
                .build();
        //写入拼团记录
        try {
            groupBuyOrderListDao.insert(groupBuyOrderListReq);
        }catch (DuplicateKeyException e)
        {
         throw new AppException(ResponseCode.INDEX_EXCEPTION);
        }

        return MarketPayOrderEntity.builder()
                .teamId(teamId)
                .orderId(orderId)
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .build();
    }

    @Override
    public Integer queryOrderCountByActivityId(String userId, Long activityId) {
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(userId);
        groupBuyOrderListReq.setActivityId(activityId);

        return groupBuyOrderListDao.queryOrderCountByActivityId(groupBuyOrderListReq);
    }

    @Override
    public GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId) {
        GroupBuyOrder groupBuyOrder =groupBuyOrderDao.queryGroupBuyTeamByTeamId(teamId);

        return GroupBuyTeamEntity.builder()
                .teamId(groupBuyOrder.getTeamId())
                .activityId(groupBuyOrder.getActivityId())
                .targetCount(groupBuyOrder.getTargetCount())
                .completeCount(groupBuyOrder.getCompleteCount())
                .lockCount(groupBuyOrder.getLockCount())
                .status(GroupBuyOrderEnumVO.valueOf(groupBuyOrder.getStatus()) )
                .validStartTime(groupBuyOrder.getValidStartTime())
                .validEndTime(groupBuyOrder.getValidEndTime())
                .notifyUrl(groupBuyOrder.getNotifyUrl())
                .build();
    }


    @Transactional(timeout = 500)
    @Override
    public boolean settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate) {
        UserEntity userEntity = groupBuyTeamSettlementAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = groupBuyTeamSettlementAggregate.getGroupBuyTeamEntity();
        TradePaySuccessEntity tradePaySuccessEntity = groupBuyTeamSettlementAggregate.getTradePaySuccessEntity();

        //1.更新拼团明细状态
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(userEntity.getUserId());
        groupBuyOrderListReq.setOutTradeNo(tradePaySuccessEntity.getOutTradeNo());
        groupBuyOrderListReq.setOutTradeTime(tradePaySuccessEntity.getOutTradeTime());

        int updateOrderListStatusCount=groupBuyOrderListDao.updateOrderStatus2COMPLETE(groupBuyOrderListReq);
        if (1!=updateOrderListStatusCount){
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        int updateAddCount =groupBuyOrderDao.updateAddCompleteCount(groupBuyTeamEntity.getTeamId());
        if (1!=updateAddCount){
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        if (groupBuyTeamEntity.getTargetCount() - groupBuyTeamEntity.getCompleteCount() == 1) {
            int updateOrderStatusCount = groupBuyOrderDao.updateOrderStatus2COMPLETE(groupBuyTeamEntity.getTeamId());
            if (1 != updateOrderStatusCount) {
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }

            // 查询拼团交易完成外部单号列表
            List<String> outTradeNoList = groupBuyOrderListDao.queryGroupBuyCompleteOrderOutTradeNoListByTeamId(groupBuyTeamEntity.getTeamId());

            // 拼团完成写入回调任务记录
            NotifyTask notifyTask = new NotifyTask();
            notifyTask.setActivityId(groupBuyTeamEntity.getActivityId());
            notifyTask.setTeamId(groupBuyTeamEntity.getTeamId());
            notifyTask.setNotifyUrl(groupBuyTeamEntity.getNotifyUrl());
            notifyTask.setNotifyCount(0);
            notifyTask.setNotifyStatus(0);

            notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                put("teamId", groupBuyTeamEntity.getTeamId());
                put("outTradeNoList", outTradeNoList);
            }}));

            notifyTaskDao.insert(notifyTask);

            return true;
        }

        return false;
    }

    @Override
    public boolean isSCBlackIntercepter(String source, String channel) {


        return dccService.isScBlacklist(source, channel);
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecuteNotifyTaskList(String teamId) {
        NotifyTask notifyTask = notifyTaskDao.queryUnExecutedNotifyTaskByTeamId(teamId);
        if (null == notifyTask) return new ArrayList<>();
        return Collections.singletonList(NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyUrl(notifyTask.getNotifyUrl())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build());

    }

    @Override
    public List<NotifyTaskEntity> queryUnExecuteNotifyTaskList() {
        List<NotifyTask> notifyTaskList = notifyTaskDao.queryUnExecutedNotifyTaskList();
        if (notifyTaskList.isEmpty()) return new ArrayList<>();

        List<NotifyTaskEntity> notifyTaskEntities = new ArrayList<>();
        for (NotifyTask notifyTask : notifyTaskList) {

            NotifyTaskEntity notifyTaskEntity = NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .build();

            notifyTaskEntities.add(notifyTaskEntity);
        }

        return notifyTaskEntities;
    }

    @Override
    public int updateNotifyStatusSuccess(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusSuccess(teamId);
    }

    @Override
    public int updateNotifyStatusError(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusError(teamId);
    }

    @Override
    public int updateNotifyStatusRetry(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusRetry(teamId);
    }

    @Override
    public GroupBuyActivityEntity queryGroupBuyActivityByActivityId(Long activityId) {
        GroupBuyActivity groupBuyActivity = groupBuyActivityDao.queryGroupBuyActivityById(activityId);

        return GroupBuyActivityEntity.builder()
                .activityId(groupBuyActivity.getActivityId())
                .activityName(groupBuyActivity.getActivityName())
                .discountId(groupBuyActivity.getDiscountId())
                .groupType(groupBuyActivity.getGroupType())
                .takeLimitCount(groupBuyActivity.getTakeLimitCount())
                .target(groupBuyActivity.getTarget())
                .validTime(groupBuyActivity.getValidTime())
                .status(ActivityStatusEnumVO.valueOf(groupBuyActivity.getStatus()))
                .startTime(groupBuyActivity.getStartTime())
                .endTime(groupBuyActivity.getEndTime())
                .tagId(groupBuyActivity.getTagId())
                .tagScope(groupBuyActivity.getTagScope())
                .build();
    }
}

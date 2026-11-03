package com.hxy.domain.trade.service.settlement;

import com.alibaba.fastjson.JSON;
import com.hxy.domain.trade.adapter.port.ITradePort;
import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hxy.domain.trade.model.entity.*;
import com.hxy.domain.trade.service.ITradeSettlementOrderService;
import com.hxy.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import com.hxy.types.enums.NotifyTaskHTTPEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
public class TradeSettlementOrderService implements ITradeSettlementOrderService {

    @Resource
    private ITradeRepository repository;
    @Resource
    private ITradePort port;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;


    @Resource
    private BusinessLinkedList<TradeSettlementRuleCommandEntity,
            TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter;

    @Override
    public TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception {

        //1.查询拼团信息

        TradeSettlementRuleFilterBackEntity tradeSettlementRuleFilterBackEntity = tradeSettlementRuleFilter.apply(TradeSettlementRuleCommandEntity.builder()
                .source(tradePaySuccessEntity.getSource())
                .channel(tradePaySuccessEntity.getChannel())
                .userId(tradePaySuccessEntity.getUserId())
                .outTradeNo(tradePaySuccessEntity.getOutTradeNo())
                .outTradeTime(tradePaySuccessEntity.getOutTradeTime())
                .build(), new TradeSettlementRuleFilterFactory.DynamicContext());

        //2.查询拼团状态
        GroupBuyTeamEntity groupBuyTeamEntity = GroupBuyTeamEntity.builder()
                .teamId(tradeSettlementRuleFilterBackEntity.getTeamId())
                .activityId(tradeSettlementRuleFilterBackEntity.getActivityId())
                .targetCount(tradeSettlementRuleFilterBackEntity.getTargetCount())
                .completeCount(tradeSettlementRuleFilterBackEntity.getCompleteCount())
                .lockCount(tradeSettlementRuleFilterBackEntity.getLockCount())
                .status(tradeSettlementRuleFilterBackEntity.getStatus())
                .validStartTime(tradeSettlementRuleFilterBackEntity.getValidStartTime())
                .validEndTime(tradeSettlementRuleFilterBackEntity.getValidEndTime())
                .notifyConfigVO(tradeSettlementRuleFilterBackEntity.getNotifyConfig())
                .build();

        GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate = GroupBuyTeamSettlementAggregate.builder()
                .userEntity(UserEntity.builder().userId(tradePaySuccessEntity.getUserId()).build())
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .tradePaySuccessEntity(tradePaySuccessEntity)
                .build();

        //拼团交易结算
        NotifyTaskEntity notifyTaskEntity = repository.settlementMarketPayOrder(groupBuyTeamSettlementAggregate);

        // 5. 组队回调处理 - 处理失败也会有定时任务补偿，通过这样的方式，可以减轻任务调度，提高时效性
        if (null != notifyTaskEntity) {
            try {
                threadPoolExecutor.execute(() -> {
                    try {
                        Map<String, Integer> result = execSettlementNotifyJob(notifyTaskEntity);
                        log.info("回调通知拼团完结 result:{}", JSON.toJSONString(result));
                    } catch (Exception e) {
                        log.error("拼团通知即时执行失败，等待定时补偿 teamId:{}", notifyTaskEntity.getTeamId(), e);
                    }
                });
            } catch (RejectedExecutionException e) {
                log.warn("拼团通知线程池繁忙，等待定时补偿 teamId:{}", notifyTaskEntity.getTeamId(), e);
            }

        }
        return TradePaySettlementEntity.builder()
                .source(tradePaySuccessEntity.getSource())
                .channel(tradePaySuccessEntity.getChannel())
                .userId(tradePaySuccessEntity.getUserId())
                .teamId(tradeSettlementRuleFilterBackEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .outTradeNo(tradePaySuccessEntity.getOutTradeNo())
                .build();
    }

    @Override
    public Map<String, Integer> execSettlementNotifyJob() throws Exception {
        log.info("拼团交易-执行结算通知任务");

        List<NotifyTaskEntity>  notifyTaskEntityList =repository.queryUnExecuteNotifyTaskList();

        return execSettlementNotifyJob(notifyTaskEntityList);


    }

    private Map<String, Integer> execSettlementNotifyJob(List<NotifyTaskEntity> notifyTaskEntityList) throws Exception {
        int successCount = 0,errorCount = 0,retryCount = 0;

        for (NotifyTaskEntity notifyTaskEntity : notifyTaskEntityList) {
            String response;
            try {
                response = port.groupBuyNotify(notifyTaskEntity);
            } catch (Exception e) {
                log.error("拼团通知失败 teamId:{}", notifyTaskEntity.getTeamId(), e);
                response = NotifyTaskHTTPEnumVO.ERROR.getCode();
            }
            // 未抢到锁，本轮不计为发送失败。其他非 success 返回均按失败处理。
            if (response == null) continue;
            if (NotifyTaskHTTPEnumVO.SUCCESS.getCode().equals(response)) {
                int updateCount = repository.updateNotifyStatusSuccess(notifyTaskEntity.getTeamId());
                if (1 == updateCount){
                    successCount +=1;
                }
            } else {
                if (notifyTaskEntity.getNotifyCount() == null || notifyTaskEntity.getNotifyCount() < 4){
                    int updateCount = repository.updateNotifyStatusRetry(notifyTaskEntity.getTeamId());
                    if (1 == updateCount){
                        retryCount +=1;
                    }
                }else{
                    int updateCount = repository.updateNotifyStatusError(notifyTaskEntity.getTeamId());
                    if (1 == updateCount){
                        errorCount +=1;
                    }

                }

            }
        }

        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("waitCount", notifyTaskEntityList.size());
        resultMap.put("successCount", successCount);
        resultMap.put("errorCount", errorCount);
        resultMap.put("retryCount", retryCount);

        return resultMap;
    }

    @Override
    public Map<String, Integer> execSettlementNotifyJob(String teamId) throws Exception {
        log.info("拼团交易-执行结算通知任务,指定teamId={}", teamId);

        List<NotifyTaskEntity> notifyTaskEntities = repository.queryUnExecuteNotifyTaskList(teamId);
        return execSettlementNotifyJob(notifyTaskEntities);

    }

    @Override
    public Map<String, Integer> execSettlementNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception {
        log.info("拼团交易-执行结算通知回调，指定 teamId:{} notifyTaskEntity:{}", notifyTaskEntity.getTeamId(), JSON.toJSONString(notifyTaskEntity));
        return execSettlementNotifyJob(Collections.singletonList(notifyTaskEntity));
    }
}

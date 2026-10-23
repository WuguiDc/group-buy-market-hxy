package com.hxy.trigger.job;


import com.hxy.domain.trade.service.ITradeSettlementOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
@Slf4j
public class GroupBuyNotifyJob {

    @Resource
    private ITradeSettlementOrderService tradeSettlementOrderService;

    @Scheduled(cron = "0/15 * * * * ?")
    public void exec(){
        try {
            Map<String, Integer> result = tradeSettlementOrderService.execSettlementNotifyJob();
            log.info("定时任务，回调通知拼团完结任务 result:{}" , result);
        }catch (Exception e){
            log.error("定时任务，回调通知拼团完结任务失败",e);
        }

    }
}

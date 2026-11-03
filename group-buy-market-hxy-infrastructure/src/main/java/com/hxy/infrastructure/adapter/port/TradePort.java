package com.hxy.infrastructure.adapter.port;

import com.hxy.domain.trade.adapter.port.ITradePort;
import com.hxy.domain.trade.model.entity.NotifyTaskEntity;
import com.hxy.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.hxy.infrastructure.event.EventPublisher;
import com.hxy.infrastructure.gateway.GroupBuyNotifyService;
import com.hxy.infrastructure.redis.IRedisService;
import com.hxy.types.enums.NotifyTaskHTTPEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class TradePort implements ITradePort {

    @Resource
    private GroupBuyNotifyService groupBuyNotifyService;

    @Resource
    private IRedisService redisService;

    @Resource
    private EventPublisher publisher;

    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTask) throws Exception {
        RLock lock = redisService.getLock(notifyTask.lockKey());
        try {
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    // 回调方式 HTTP
                    if (NotifyTypeEnumVO.HTTP.getCode().equals(notifyTask.getNotifyType())) {
                        // 缺少回调地址不能视作已通知，交给任务重试并记录失败。
                        if (StringUtils.isBlank(notifyTask.getNotifyUrl()) || "暂无".equals(notifyTask.getNotifyUrl())) {
                            return NotifyTaskHTTPEnumVO.ERROR.getCode();
                        }
                        return groupBuyNotifyService.groupBuyNotify(notifyTask.getNotifyUrl(), notifyTask.getParameterJson());
                    }

                    // 回调方式 MQ
                    if (NotifyTypeEnumVO.MQ.getCode().equals(notifyTask.getNotifyType())) {
                        if (StringUtils.isBlank(notifyTask.getNotifyMQ())) return NotifyTaskHTTPEnumVO.ERROR.getCode();
                        publisher.publish(notifyTask.getNotifyMQ(), notifyTask.getParameterJson());
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }
                    return NotifyTaskHTTPEnumVO.ERROR.getCode();
                }finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }

            }
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        } catch (Exception e) {
            log.error("拼团通知发送失败 teamId:{}", notifyTask.getTeamId(), e);
            return NotifyTaskHTTPEnumVO.ERROR.getCode();
        }

    }


}

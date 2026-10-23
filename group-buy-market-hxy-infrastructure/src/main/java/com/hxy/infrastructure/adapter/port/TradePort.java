package com.hxy.infrastructure.adapter.port;

import com.hxy.domain.trade.adapter.port.ITradePort;
import com.hxy.domain.trade.model.entity.NotifyTaskEntity;
import com.hxy.infrastructure.gateway.GroupBuyNotifyService;
import com.hxy.infrastructure.redis.IRedisService;
import com.hxy.types.enums.NotifyTaskHTTPEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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

    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTaskEntity) throws Exception {
        RLock lock = redisService.getLock(notifyTaskEntity.lockKey());
        try {
            if (lock.tryLock(3,0, TimeUnit.SECONDS)) {
                try {
                    if (StringUtils.isBlank(notifyTaskEntity.getNotifyUrl()) || "暂无".equals(notifyTaskEntity.getNotifyUrl())) {
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }
                    return groupBuyNotifyService.groupBuyNotify(notifyTaskEntity.getNotifyUrl(),notifyTaskEntity.getParameterJson());
                }finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }

            }
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        }catch (Exception e){
            Thread.currentThread().interrupt();
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        }

    }


}

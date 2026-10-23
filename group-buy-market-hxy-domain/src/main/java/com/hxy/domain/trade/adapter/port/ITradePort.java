package com.hxy.domain.trade.adapter.port;

import com.hxy.domain.trade.model.entity.NotifyTaskEntity;

public interface ITradePort {

    String groupBuyNotify(NotifyTaskEntity notifyTaskEntity) throws Exception;

}

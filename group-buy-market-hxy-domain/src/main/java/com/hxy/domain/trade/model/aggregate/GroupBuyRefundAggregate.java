package com.hxy.domain.trade.model.aggregate;


import com.hxy.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hxy.domain.trade.model.valobj.GroupBuyProgressVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyRefundAggregate {


    /**
     * 交易退单
     */
    private TradeRefundOrderEntity tradeRefundOrderEntity;

    /**
     * 退单进度
     */
    private GroupBuyProgressVO groupBuyProgressVO;

    public static GroupBuyRefundAggregate buildUnpaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity, Integer lockCount) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgressVO(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .build());
        return groupBuyRefundAggregate;
    }

}

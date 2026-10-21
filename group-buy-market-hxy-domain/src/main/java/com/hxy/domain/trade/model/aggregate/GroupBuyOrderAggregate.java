package com.hxy.domain.trade.model.aggregate;


import com.hxy.domain.trade.model.entity.MarketPayOrderEntity;
import com.hxy.domain.trade.model.entity.PayActivityEntity;
import com.hxy.domain.trade.model.entity.PayDiscountEntity;
import com.hxy.domain.trade.model.entity.UserEntity;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderAggregate {

    private UserEntity userEntity;

    private PayActivityEntity payActivityEntity;

    private PayDiscountEntity payDiscountEntity;

    private Integer userTakeOrderCount;
}

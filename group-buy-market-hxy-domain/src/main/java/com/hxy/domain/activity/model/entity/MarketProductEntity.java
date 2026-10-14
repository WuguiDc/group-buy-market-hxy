package com.hxy.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
* @Description: 营销商品实体信息
* @Author: hxy
* @Date: 2026/10/12
*/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketProductEntity {

    /** 用户ID */
    private String userId;
    /** 商品ID */
    private String goodsId;
    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;

}

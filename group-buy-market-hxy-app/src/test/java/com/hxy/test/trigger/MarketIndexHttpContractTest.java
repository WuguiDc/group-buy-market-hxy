package com.hxy.test.trigger;

import com.hxy.domain.activity.model.entity.MarketProductEntity;
import com.hxy.domain.activity.model.entity.TrialBalanceEntity;
import com.hxy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hxy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hxy.domain.activity.model.valobj.TeamStatisticVO;
import com.hxy.domain.activity.service.IIndexGroupBuyMarketService;
import com.hxy.trigger.http.MarketIndexController;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MarketIndexHttpContractTest {
    @Test
    public void emptyTeamsStillReturnTheTrialActivityId() throws Exception {
        IIndexGroupBuyMarketService service = mock(IIndexGroupBuyMarketService.class);
        when(service.indexMarketTrial(any())).thenReturn(TrialBalanceEntity.builder()
                .goodsId("9890001").originalPrice(new BigDecimal("100.00"))
                .deductionPrice(new BigDecimal("10.00")).payPrice(new BigDecimal("90.00"))
                .groupBuyActivityDiscountVO(GroupBuyActivityDiscountVO.builder().activityId(998877L).build()).build());
        when(service.queryInProgressUserGroupBuyOrderDetailList(998877L, "new-user", 1, 2)).thenReturn(Collections.emptyList());
        when(service.queryTeamStatisticByActivityId(998877L)).thenReturn(new TeamStatisticVO(0, 0, 0));
        MarketIndexController controller = new MarketIndexController();
        ReflectionTestUtils.setField(controller, "indexGroupBuyMarketService", service);
        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/v1/gbm/index/query_group_buy_market_config").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"new-user\",\"source\":\"s01\",\"channel\":\"c01\",\"goodsId\":\"9890001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.activityId").value(998877))
                .andExpect(jsonPath("$.data.teamList").isEmpty());
    }

    @Test
    public void marketReturnsPricesAndOwnerTradeStatus() throws Exception {
        IIndexGroupBuyMarketService service = mock(IIndexGroupBuyMarketService.class);
        when(service.indexMarketTrial(any())).thenReturn(TrialBalanceEntity.builder()
                .goodsId("9890001").originalPrice(new BigDecimal("100.00"))
                .deductionPrice(new BigDecimal("10.25")).payPrice(new BigDecimal("89.75"))
                .groupBuyActivityDiscountVO(GroupBuyActivityDiscountVO.builder().activityId(100123L).build()).build());
        when(service.queryInProgressUserGroupBuyOrderDetailList(100123L, "xfg01", 1, 2))
                .thenReturn(Collections.singletonList(UserGroupBuyOrderDetailEntity.builder()
                        .userId("xfg01").teamId("team01").activityId(100123L).outTradeNo("202610240001")
                        .targetCount(3).lockCount(1).completeCount(1).tradeOrderStatus(1)
                        .validStartTime(new Date()).validEndTime(new Date(System.currentTimeMillis() + 600000)).build()));
        when(service.queryTeamStatisticByActivityId(100123L)).thenReturn(new TeamStatisticVO(7, 1, 9));
        MarketIndexController controller = new MarketIndexController();
        ReflectionTestUtils.setField(controller, "indexGroupBuyMarketService", service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/api/v1/gbm/index/query_group_buy_market_config").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"xfg01\",\"source\":\"s01\",\"channel\":\"c01\",\"goodsId\":\"9890001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.activityId").value(100123))
                .andExpect(jsonPath("$.data.goods.payPrice").value(89.75))
                .andExpect(jsonPath("$.data.teamStatistic.allTeamUserCount").value(9))
                .andExpect(jsonPath("$.data.teamList[0].tradeOrderStatus").value(1))
                .andExpect(jsonPath("$.data.teamList[0].outTradeNo").value("202610240001"));
        ArgumentCaptor<MarketProductEntity> argument = ArgumentCaptor.forClass(MarketProductEntity.class);
        verify(service).indexMarketTrial(argument.capture());
        assertEquals("xfg01", argument.getValue().getUserId());
        assertEquals("9890001", argument.getValue().getGoodsId());
    }
}

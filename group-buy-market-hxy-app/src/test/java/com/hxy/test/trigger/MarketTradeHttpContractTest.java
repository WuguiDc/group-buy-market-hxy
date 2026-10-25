package com.hxy.test.trigger;

import com.hxy.domain.trade.model.entity.TradePaySettlementEntity;
import com.hxy.domain.trade.model.entity.TradePaySuccessEntity;
import com.hxy.domain.trade.service.ITradeSettlementOrderService;
import com.hxy.trigger.http.MarketTradeController;
import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP 合约测试：不启动 Spring 上下文、不连接数据库、不写入拼团订单。 */
public class MarketTradeHttpContractTest {
    private static final String URL = "/api/v1/gbm/trade/settlement_market_pay_order";
    private static final String BODY = "{\"source\":\"s01\",\"channel\":\"c01\",\"userId\":\"xfg01\","
            + "\"outTradeNo\":\"202610240001\",\"outTradeTime\":\"2026-10-24T04:30:00.000Z\"}";
    private ITradeSettlementOrderService service;
    private MockMvc mvc;

    @Before
    public void setUp() {
        service = mock(ITradeSettlementOrderService.class);
        MarketTradeController controller = new MarketTradeController();
        ReflectionTestUtils.setField(controller, "tradeSettlementOrderService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void settlementBindsJsonAndReturnsServiceResult() throws Exception {
        when(service.settlementMarketPayOrder(any())).thenReturn(TradePaySettlementEntity.builder()
                .userId("xfg01").teamId("team01").activityId(100123L).outTradeNo("202610240001").build());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.teamId").value("team01"))
                .andExpect(jsonPath("$.data.outTradeNo").value("202610240001"));
        ArgumentCaptor<TradePaySuccessEntity> argument = ArgumentCaptor.forClass(TradePaySuccessEntity.class);
        verify(service).settlementMarketPayOrder(argument.capture());
        assertEquals("xfg01", argument.getValue().getUserId());
        assertEquals("s01", argument.getValue().getSource());
        assertEquals("c01", argument.getValue().getChannel());
        assertEquals(Instant.parse("2026-10-24T04:30:00Z").toEpochMilli(), argument.getValue().getOutTradeTime().getTime());
    }

    @Test
    public void settlementRejectsMissingFieldsWithoutCallingDomain() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0002"));
        verifyNoInteractions(service);
    }

    @Test
    public void settlementReturnsBusinessFailure() throws Exception {
        when(service.settlementMarketPayOrder(any())).thenThrow(new AppException(ResponseCode.E0104));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("E0104"));
    }

    @Test
    public void settlementRequiresJsonBody() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}

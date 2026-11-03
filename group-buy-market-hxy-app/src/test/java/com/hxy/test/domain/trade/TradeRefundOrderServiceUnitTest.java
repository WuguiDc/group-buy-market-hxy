package com.hxy.test.domain.trade;

import com.hxy.domain.trade.adapter.repository.ITradeRepository;
import com.hxy.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hxy.domain.trade.model.entity.*;
import com.hxy.domain.trade.model.valobj.RefundTypeEnumVO;
import com.hxy.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hxy.domain.trade.service.refund.TradeRefundOrderService;
import com.hxy.domain.trade.service.refund.business.impl.Unpaid2RefundStrategy;
import com.hxy.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hxy.domain.trade.service.settlement.filter.OutTradeNoRuleFilter;
import com.hxy.infrastructure.adapter.repository.TradeRepository;
import com.hxy.infrastructure.dao.IGroupBuyOrderDao;
import com.hxy.infrastructure.dao.IGroupBuyOrderListDao;
import com.hxy.infrastructure.dao.po.GroupBuyOrder;
import com.hxy.infrastructure.dao.po.GroupBuyOrderList;
import com.hxy.types.enums.GroupBuyOrderEnumVO;
import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 不启动 Spring，也不连接 MySQL、Redis、MQ。 */
public class TradeRefundOrderServiceUnitTest {

    private final ITradeRepository repository = mock(ITradeRepository.class);
    private final TradeRefundCommandEntity command = TradeRefundCommandEntity.builder()
            .userId("user01").outTradeNo("123456789012").source("s01").channel("c01").build();

    private MarketPayOrderEntity order(TradeOrderStatusEnumVO status) {
        return MarketPayOrderEntity.builder().teamId("team01").orderId("order01")
                .tradeOrderStatusEnumVO(status).build();
    }

    @Test
    public void unpaidRefundClosesOrderAndReleasesOnePlace() {
        when(repository.queryMarketPayOrderByOutTradeNo("user01", "123456789012"))
                .thenReturn(order(TradeOrderStatusEnumVO.CREATE), order(TradeOrderStatusEnumVO.CLOSE));
        when(repository.queryGroupBuyTeamByTeamId("team01"))
                .thenReturn(GroupBuyTeamEntity.builder().status(GroupBuyOrderEnumVO.PROGRESS).build());
        Unpaid2RefundStrategy strategy = new Unpaid2RefundStrategy();
        ReflectionTestUtils.setField(strategy, "repository", repository);
        TradeRefundOrderService service = new TradeRefundOrderService(repository,
                Collections.singletonMap("unpaid2RefundStrategy", strategy));

        assertEquals(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.SUCCESS,
                service.refundOrder(command).getTradeRefundBehaviorEnum());
        assertEquals(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.REPEAT,
                service.refundOrder(command).getTradeRefundBehaviorEnum());

        ArgumentCaptor<GroupBuyRefundAggregate> captor = ArgumentCaptor.forClass(GroupBuyRefundAggregate.class);
        verify(repository, times(1)).unpaid2Refund(captor.capture());
        assertEquals(Integer.valueOf(-1), captor.getValue().getGroupBuyProgressVO().getLockCount());
        assertEquals("user01", captor.getValue().getTradeRefundOrderEntity().getUserId());
        assertEquals("team01", captor.getValue().getTradeRefundOrderEntity().getTeamId());
        assertEquals("order01", captor.getValue().getTradeRefundOrderEntity().getOrderId());
        verify(repository, times(1)).queryGroupBuyTeamByTeamId("team01");
    }

    @Test
    public void missingOrderReturnsBusinessError() {
        TradeRefundOrderService service = new TradeRefundOrderService(repository, Collections.emptyMap());
        try {
            service.refundOrder(command);
            fail("不存在的订单不应退单成功");
        } catch (AppException e) {
            assertEquals(ResponseCode.E0104.getCode(), e.getCode());
        }
        verify(repository, never()).unpaid2Refund(any());
    }

    @Test
    public void strategiesMatchOrderAndTeamStates() {
        assertEquals(RefundTypeEnumVO.UNPAID_UNLOCK,
                RefundTypeEnumVO.getRefundStrategy(GroupBuyOrderEnumVO.PROGRESS, TradeOrderStatusEnumVO.CREATE));
        assertEquals(RefundTypeEnumVO.PAID_UNFORMED,
                RefundTypeEnumVO.getRefundStrategy(GroupBuyOrderEnumVO.PROGRESS, TradeOrderStatusEnumVO.COMPLETE));
        assertEquals(RefundTypeEnumVO.PAID_FORMED,
                RefundTypeEnumVO.getRefundStrategy(GroupBuyOrderEnumVO.COMPLETE, TradeOrderStatusEnumVO.COMPLETE));
    }

    @Test(expected = RuntimeException.class)
    public void unsupportedStatesDoNotSelectARefundStrategy() {
        RefundTypeEnumVO.getRefundStrategy(GroupBuyOrderEnumVO.COMPLETE, TradeOrderStatusEnumVO.CREATE);
    }

    private void updateRefund(int detailRows, int teamRows) {
        TradeRepository tradeRepository = new TradeRepository();
        IGroupBuyOrderListDao detailDao = mock(IGroupBuyOrderListDao.class);
        IGroupBuyOrderDao teamDao = mock(IGroupBuyOrderDao.class);
        ReflectionTestUtils.setField(tradeRepository, "groupBuyOrderListDao", detailDao);
        ReflectionTestUtils.setField(tradeRepository, "groupBuyOrderDao", teamDao);
        when(detailDao.unpaid2Refund(any())).thenReturn(detailRows);
        when(teamDao.unpaid2Refund(any())).thenReturn(teamRows);
        GroupBuyRefundAggregate aggregate = GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(
                TradeRefundOrderEntity.builder().userId("user01").orderId("order01").teamId("team01").build(), -1);
        if (detailRows == 1 && teamRows == 1) {
            tradeRepository.unpaid2Refund(aggregate);
            ArgumentCaptor<GroupBuyOrder> team = ArgumentCaptor.forClass(GroupBuyOrder.class);
            verify(teamDao).unpaid2Refund(team.capture());
            assertEquals("team01", team.getValue().getTeamId());
            assertEquals(Integer.valueOf(-1), team.getValue().getLockCount());
            ArgumentCaptor<GroupBuyOrderList> detail = ArgumentCaptor.forClass(GroupBuyOrderList.class);
            verify(detailDao).unpaid2Refund(detail.capture());
            assertEquals("user01", detail.getValue().getUserId());
            assertEquals("order01", detail.getValue().getOrderId());
        } else {
            try {
                tradeRepository.unpaid2Refund(aggregate);
                fail("更新失败必须抛异常以触发事务回滚");
            } catch (AppException e) {
                assertEquals(ResponseCode.UPDATE_ZERO.getCode(), e.getCode());
            }
            if (detailRows != 1) verify(teamDao, never()).unpaid2Refund(any());
        }
    }

    @Test
    public void oneUpdatedRowIsSuccess() {
        updateRefund(1, 1);
    }

    @Test
    public void unchangedDetailStopsRefund() {
        updateRefund(0, 1);
    }

    @Test
    public void unchangedTeamFailsRefund() {
        updateRefund(1, 0);
    }

    @Test
    public void closedOrderCannotBeSettled() throws Exception {
        when(repository.queryMarketPayOrderByOutTradeNo("user01", "123456789012"))
                .thenReturn(order(TradeOrderStatusEnumVO.CLOSE));
        OutTradeNoRuleFilter filter = new OutTradeNoRuleFilter();
        ReflectionTestUtils.setField(filter, "repository", repository);
        try {
            filter.apply(TradeSettlementRuleCommandEntity.builder().userId("user01")
                    .outTradeNo("123456789012").build(), new TradeSettlementRuleFilterFactory.DynamicContext());
            fail("已退单订单不能结算");
        } catch (AppException e) {
            assertEquals(ResponseCode.E0104.getCode(), e.getCode());
        }
    }

    @Test
    public void refundMappersAreBoundToDaoMethods() throws Exception {
        Configuration configuration = new Configuration();
        for (String name : new String[]{"group_buy_order_list_mapper.xml", "group_buy_order_mapper.xml"}) {
            String resource = "mybatis/mapper/" + name;
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(resource, input);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        assertTrue(configuration.hasStatement(IGroupBuyOrderListDao.class.getName() + ".unpaid2Refund"));
        assertTrue(configuration.hasStatement(IGroupBuyOrderDao.class.getName() + ".unpaid2Refund"));
    }
}

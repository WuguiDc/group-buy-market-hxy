package com.hxy.api;

import com.hxy.api.dto.LockMarketPayOrderRequestDTO;
import com.hxy.api.dto.LockMarketPayOrderResponseDTO;
import com.hxy.api.dto.SettlementMarketPayOrderRequestDTO;
import com.hxy.api.dto.SettlementMarketPayOrderResponseDTO;
import com.hxy.api.response.Response;

public interface IMarketTradeService {

    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO);

    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(SettlementMarketPayOrderRequestDTO settlementMarketPayOrderRequestDTO);
}

package com.hxy.api;

import com.hxy.api.dto.GoodsMarketRequestDTO;
import com.hxy.api.dto.GoodsMarketResponseDTO;
import com.hxy.api.response.Response;

public interface IMarketIndexService {

    Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(GoodsMarketRequestDTO requestDTO);
}

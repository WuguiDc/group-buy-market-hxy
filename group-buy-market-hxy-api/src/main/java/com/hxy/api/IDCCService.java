package com.hxy.api;

import com.hxy.api.response.Response;

public interface IDCCService {

    Response<Boolean> updateConfig(String key, String value);
}

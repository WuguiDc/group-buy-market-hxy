package com.hxy.infrastructure.gateway;


import com.hxy.types.enums.ResponseCode;
import com.hxy.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class GroupBuyNotifyService {

    @Resource
    private OkHttpClient okHttpClient;

    public String groupBuyNotify(String apiUrl , String notifyRequestDTOJSON) throws Exception{
        try {
            // 1. 构建参数
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, notifyRequestDTOJSON);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .build();

            // 2. 调用接口
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new AppException(ResponseCode.HTTP_EXCEPTION);
                }
                return response.body().string().trim();
            }

        }catch (Exception e){
            throw new AppException(ResponseCode.HTTP_EXCEPTION);
        }

    }
}

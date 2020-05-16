package com.sagacity.docs.service;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.weixin.sdk.api.AccessTokenApi;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.utils.HttpUtils;
import com.jfinal.wxaapp.api.WxaAccessTokenApi;

public class ContentCheckApi {

    private static String apiUrl = "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=";

    public static ApiResult msgCheck(String content) {
        JSONObject jo = new JSONObject();
        jo.put("content", content);
        String jsonResult = HttpUtils.post(apiUrl + WxaAccessTokenApi.getAccessTokenStr(), jo.toJSONString());
        return new ApiResult(jsonResult);
    }
}

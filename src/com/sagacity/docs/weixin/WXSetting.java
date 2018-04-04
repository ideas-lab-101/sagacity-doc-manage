package com.sagacity.docs.weixin;

import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.weixin.sdk.api.ApiConfig;
import com.jfinal.weixin.sdk.utils.HttpUtils;
import com.sagacity.docs.utility.PropertiesFactoryHelper;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mulaliu on 17/11/26.
 */
public class WXSetting {

    public static final WXSetting dao = new WXSetting();

    public String getAccessToken(){
        if(CacheKit.get("AccessTokenCache", "token") != null){
            return CacheKit.get("AccessTokenCache", "token");
        }else{
            String getApiUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
            Map<String, String> params = new HashMap<String, String>();
            params.put("appid", PropertiesFactoryHelper.getInstance()
                    .getConfig("wxss.appid"));
            params.put("secret", PropertiesFactoryHelper.getInstance()
                    .getConfig("wxss.appsecret"));
            JSONObject res = JSONObject.fromObject(HttpUtils.get(getApiUrl, params));
            CacheKit.put("AccessTokenCache", "token", res.getString("access_token"));
            return res.getString("access_token");
        }
    }
}

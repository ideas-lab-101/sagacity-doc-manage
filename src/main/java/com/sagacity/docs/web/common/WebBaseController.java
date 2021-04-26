package com.sagacity.docs.web.common;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.ehcache.CacheKit;
import com.sagacity.docs.base.extend.CacheKey;
import com.sagacity.docs.base.extend.RestResult;
import com.sagacity.docs.model.UserDao;

import java.util.HashMap;
import java.util.Map;

/**
 * @类名字：BaseController
 * @类描述：
 * @author:Carl.Wu
 * @版本信息：
 * @日期：2013-9-11
 * @Copyright 足下 Corporation 2013 
 * @版权所有
 *
 */

@Before(WebLoginInterceptor.class)
public abstract class WebBaseController extends Controller {
	
	protected int pageSize = 20;
	protected Map<String,Object> data = new HashMap<String, Object>();
	public RestResult rest = new RestResult();
	
	public abstract void index();
	
	@Override
	public void render(String view) {
		super.render(view);
	}

	public UserDao getCurrentUser(){
		String token = getCookie("token");
		JSONObject jo = CacheKit.get(CacheKey.CACHE_WEB, token);
		if(jo!= null){
			return (UserDao)jo.get("userInfo");
		}else{
			return null;
		}
	}

//	public Map<String, Object> convertPageData(Page page){
//		responseData.put(ResponseCode.CODE, 0);
//		responseData.put(ResponseCode.DATA, page.getList());
//		responseData.put(ResponseCode.TotalRow, page.getTotalRow());
//		responseData.put(ResponseCode.PageNumber, page.getPageNumber());
//		responseData.put(ResponseCode.PageSize, page.getPageSize());
//		return responseData;
//	}

}
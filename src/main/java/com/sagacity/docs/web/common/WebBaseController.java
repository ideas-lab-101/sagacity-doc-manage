package com.sagacity.docs.web.common;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.CacheKit;
import com.sagacity.docs.base.extend.CacheKey;
import com.sagacity.docs.base.extend.ResponseCode;
import net.sf.json.JSONObject;

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
	protected static String ROOTPATH = PathKit.getWebRootPath();
	protected Map<String,Object> responseData = new HashMap<String, Object>();
	
	public abstract void index();
	
	@Override
	public void render(String view) {
		super.render(view);
	}

	public JSONObject getCurrentUser(){
		String uid = getCookie("u_id");
		JSONObject jo = CacheKit.get(CacheKey.CACHE_WEB, uid);
		if(jo!= null){
			return jo.getJSONObject("UserInfo");
		}else{
			return null;
		}
	}

	public Map<String, Object> convertPageData(Page page){
		responseData.put(ResponseCode.CODE, 0);
		responseData.put(ResponseCode.DATA, page.getList());
		responseData.put(ResponseCode.TotalRow, page.getTotalRow());
		responseData.put(ResponseCode.PageNumber, page.getPageNumber());
		responseData.put(ResponseCode.PageSize, page.getPageSize());
		return responseData;
	}

}
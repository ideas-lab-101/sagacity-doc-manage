package com.sagacity.docs.web.common;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.jfinal.plugin.ehcache.CacheKit;
import com.sagacity.docs.base.extend.CacheKey;

/**
 * @类名字：UserInterceptor
 * @类描述：用户登录拦截器
 * @author:Carl.Wu
 * @版本信息：
 * @日期：2013-5-7
 * @Copyright 足下 Corporation 2013
 * @版权所有
 * 
 */
public class WebLoginInterceptor implements Interceptor {

	/**
	 * 绕过拦截器的方法名(注意:包括命名空间)
	 */
	private String[] throughMethods = {""};

	/**
	 * @方法名:intercept
	 * @方法描述： 检测session中是否有用户信息如果没有则检测cookie中的用信息
	 * @author: Carl.Wu
	 * @return: void
	 * @version: 2013-5-7 上午11:00:57
	 */
	public void intercept(Invocation arg0) {
		// TODO Auto-generated method stub
		String html_web = "<script type=\"text/javascript\"> \n" +
				"		  var flag = 'sagacity_timeout'; \n" +
				"         var topWin = window.top; \n" +
				"         topWin.location.href= '/login.html'; \n" +
				"      </script> ";
		Controller controller = arg0.getController();
        if (checkUserCache(controller) || throughIntercept(arg0.getActionKey())) {
            arg0.invoke();
        } else {
			controller.renderHtml(html_web);
        }
	}

	/**
	 * 检查ehcache中的用户信息是否存在
	 * @param controller
	 * @return
	 */
	public boolean checkUserCache(Controller controller){

		String token = controller.getCookie("token");
		JSONObject jo = CacheKit.get(CacheKey.CACHE_WEB, token);
		if (jo != null){
			return true;
		}
		return false;
	}

	/**
	 * @方法名:canVisit
	 * @方法描述：验证绕过拦截器方法列表
	 * @author: Carl.Wu
	 * @return: boolean
	 * @version: 2013-5-7 下午12:26:21
	 */
	public boolean throughIntercept(String actionKey) {
		if (throughMethods != null) {
			for (String name : throughMethods) {
				System.out.println(name);
				if(name.contains("*")){
					if (actionKey.contains(name.replace("*", ""))) {
						return true;
					}
				}else{
					if (actionKey.equals(name)) {
						return true;
					}
				}
			}
			return false;
		} else
			return true;

	}
}

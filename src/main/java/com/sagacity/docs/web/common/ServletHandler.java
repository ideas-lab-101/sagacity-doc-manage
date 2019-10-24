package com.sagacity.docs.web.common;

import com.jfinal.handler.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletHandler extends Handler {

	@Override
	public void handle(String target, HttpServletRequest request,
                       HttpServletResponse response, boolean[] isHandled) {

		int index = target.lastIndexOf(".report");
		if (index != -1)
			target = target.substring(0, index);
		nextHandler.handle(target, request, response, new boolean[]{true});
	}	

}

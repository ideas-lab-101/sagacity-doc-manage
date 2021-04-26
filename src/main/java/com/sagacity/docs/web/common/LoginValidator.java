package com.sagacity.docs.web.common;

import com.jfinal.core.Controller;
import com.jfinal.kit.StrKit;
import com.jfinal.validate.Validator;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.base.extend.RestResult;

import java.util.HashMap;
import java.util.Map;

public class LoginValidator extends Validator {

	public RestResult rest = new RestResult();
	
	@Override
    protected void validate(Controller controller) {
        validateRequiredString("account", ResponseCode.MSG, "请输入用户名！");
        validateRequiredString("password", ResponseCode.MSG, "请输入密码!");
    }

	@Override
	protected void validateRequiredString(String field, String errorKey, String errorMessage) {
		if (StrKit.isBlank(this.controller.getPara(field))){
			rest.error(errorMessage);
			this.setShortCircuit(true);
			this.addError(errorKey, errorMessage);
		}
	}
    
	@Override
    protected void handleError(Controller controller) {
		controller.keepPara("account");
		controller.renderJson(rest);
    }
}

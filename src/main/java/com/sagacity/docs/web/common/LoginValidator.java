package com.sagacity.docs.web.common;

import com.jfinal.core.Controller;
import com.jfinal.validate.Validator;
import com.sagacity.docs.base.extend.ResponseCode;

import java.util.HashMap;
import java.util.Map;

public class LoginValidator extends Validator {
    
	Map<String,Object> responseData = new HashMap<String,Object>();
	
	@Override
    protected void validate(Controller controller) {
        validateRequiredString(controller.getPara("username"), ResponseCode.MSG, "请输入用户名！");
        validateRequiredString(controller.getPara("password"), ResponseCode.MSG, "请输入密码!");
    }    
	
	@Override
    protected void validateRequiredString(String field, String errorKey, String errorMessage) {
		if (field == null || "".equals(field.trim())){
			responseData.put(errorKey, errorMessage);
			this.setShortCircuit(true);
			this.addError(errorKey, errorMessage);
		}
	}
    
	@Override
    protected void handleError(Controller controller) {
		responseData.put(ResponseCode.RESULT, false);
    	controller.renderJson(responseData);
    }
}

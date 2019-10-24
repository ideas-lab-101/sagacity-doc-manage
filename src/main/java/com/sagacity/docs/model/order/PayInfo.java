package com.sagacity.docs.model.order;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="pay_info",pkName="pay_id")
public class PayInfo extends Model<PayInfo> {
	private static final long serialVersionUID = 1L;
	public final static PayInfo dao = new PayInfo();

	
}

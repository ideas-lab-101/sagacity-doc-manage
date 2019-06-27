package com.sagacity.docs.order;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="pay_item",pkName="id")
public class PayItem extends Model<PayItem> {
	private static final long serialVersionUID = 1L;
	public final static PayItem dao = new PayItem();

	
}

package com.sagacity.docs.model.order;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;
import com.sagacity.utility.DateUtils;

@TableBind(tableName="pay_order",pkName="orderID")
public class OrderInfo extends Model<OrderInfo> {
	private static final long serialVersionUID = 1L;
	public final static OrderInfo dao = new OrderInfo();

    /*
    生成订单流水号
     */
	public String genOrderCode(String prefix){

		String orderCode = String.valueOf(DateUtils.getLongDateMilliSecond());
		return prefix + orderCode;
	}
	
}

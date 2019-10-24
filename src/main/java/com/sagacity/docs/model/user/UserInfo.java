package com.sagacity.docs.model.user;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="sys_users",pkName="UserID")
public class UserInfo extends Model<UserInfo> {
	public final static UserInfo dao = new UserInfo();

	/**
	 * 调用统一的方法创建用户
	 * @return
	 */
	public boolean createUser(){
		boolean r = false;

		return r;
	}
}

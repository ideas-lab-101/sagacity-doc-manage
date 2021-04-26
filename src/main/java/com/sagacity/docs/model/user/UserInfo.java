package com.sagacity.docs.model.user;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="sys_users",pkName="user_id")
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

	public UserInfo findByName(String account){
		UserInfo user = UserInfo.dao.findFirst("select * from sys_users where account=?", account);
		return  user;
	}
}

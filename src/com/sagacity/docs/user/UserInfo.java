package com.sagacity.docs.user;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

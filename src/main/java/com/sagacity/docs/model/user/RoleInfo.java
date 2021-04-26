package com.sagacity.docs.model.user;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="sys_roles",pkName="role_id")
public class RoleInfo extends Model<RoleInfo> {
	public final static RoleInfo dao = new RoleInfo();

	/**
	 * 调用统一的方法创建用户
	 * @return
	 */
	public boolean addRole(){
		boolean r = false;

		return r;
	}
}

package com.sagacity.docs.weixin;

import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;
import net.sf.json.JSONObject;

@TableBind(tableName="user_follow",pkName="id")
public class UserFollow extends Model<UserFollow> {

    public final static UserFollow dao = new UserFollow();

}

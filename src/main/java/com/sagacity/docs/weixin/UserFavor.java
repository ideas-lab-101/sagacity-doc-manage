package com.sagacity.docs.weixin;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="user_favor",pkName="id")
public class UserFavor extends Model<UserFavor> {

    public final static UserFavor dao = new UserFavor();

}

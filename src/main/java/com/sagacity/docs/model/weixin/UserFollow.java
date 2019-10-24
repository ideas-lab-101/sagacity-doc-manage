package com.sagacity.docs.model.weixin;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="user_follow",pkName="id")
public class UserFollow extends Model<UserFollow> {

    public final static UserFollow dao = new UserFollow();

}

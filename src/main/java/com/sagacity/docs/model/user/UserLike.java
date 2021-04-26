package com.sagacity.docs.model.user;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="user_like",pkName="id")
public class UserLike extends Model<UserLike> {

    public final static UserLike dao = new UserLike();

}

package com.sagacity.docs.weixin;

import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.utility.DateUtils;
import net.sf.json.JSONObject;

import java.util.UUID;

@TableBind(tableName="wx_user",pkName="id")
public class WXUser extends Model<WXUser> {

    public final static WXUser dao = new WXUser();

    @Before(Tx.class)
    public WXUser createUser(String openid, JSONObject userInfo){
        WXUser user = WXUser.dao.findFirst("select user.* from wx_user user where user.open_id=?", openid);
        if(user == null){ //创建新用户
            user = new WXUser().set("open_id", openid).set("nick_name", userInfo.get("nickName"))
                    .set("gender", userInfo.get("gender")).set("avatar_url", userInfo.get("avatarUrl"))
                    .set("country", userInfo.get("country")).set("city", userInfo.get("city"))
                    .set("level_id",1).set("created_at", DateUtils.nowDateTime()).set("state", 1);
            user.save();
            //设置表
        }
        return user;
    }
}

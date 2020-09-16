package com.sagacity.docs.model.weixin;

import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.utility.DateUtils;
import net.sf.json.JSONObject;

@TableBind(tableName="wx_user",pkName="id")
public class WXUser extends Model<WXUser> {

    public final static WXUser dao = new WXUser();

    @Before(Tx.class)
    public WXUser setUser(String openid, JSONObject userInfo){
        WXUser user = WXUser.dao.findFirst("select user.* from wx_user user where user.open_id=?", openid);
        if(user == null){ //创建新用户
            System.out.println("=========创建新的微信用户=========");
            user = new WXUser().set("open_id", openid).set("nick_name", userInfo.get("nickName"))
                    .set("gender", userInfo.get("gender")).set("avatar_url", userInfo.get("avatarUrl"))
                    .set("country", userInfo.get("country")).set("city", userInfo.get("city"))
                    .set("level_id",1).set("created_at", DateUtils.nowDateTime()).set("state", 1);
            user.save();
        }else{ //更新用户
            System.out.println("=========更新已有用户信息=========");
            user.set("nick_name", userInfo.get("nickName")).set("gender", userInfo.get("gender")).set("avatar_url", userInfo.get("avatarUrl"))
                    .set("country", userInfo.get("country")).set("city", userInfo.get("city"))
                    .set("updated_at", DateUtils.nowDateTime());
            user.update();
        }
        return user;
    }
}

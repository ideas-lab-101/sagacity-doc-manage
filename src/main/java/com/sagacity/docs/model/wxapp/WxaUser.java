package com.sagacity.docs.model.wxapp;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.base.extend.Consts;
import com.sagacity.docs.base.extend.RoleType;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.user.RoleInfo;
import com.sagacity.docs.model.user.UserInfo;
import com.sagacity.docs.model.user.UserProfile;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;

import java.util.UUID;

@TableBind(tableName="wxapp_user",pkName="id")
public class WxaUser extends Model<WxaUser> {

    public final static WxaUser dao = new WxaUser();

    public WxaUser findByOpenId(String openId){
        WxaUser wu = WxaUser.dao.findFirst("select user.* from wxapp_user user where user.open_id=?", openId);
        return  wu;
    }

    @Before(Tx.class)
    public UserDao setWxappUser(String openid, JSONObject userData){

        WxaUser wu = findByOpenId(openid);
        if(wu == null){ //创建新用户
            System.out.println("=========创建新的微信用户=========");
            String userId = UUID.randomUUID().toString();
            UserInfo ui = new UserInfo().set("user_id", userId).set("role_id", RoleType.USER)
                    .set("account", "").set("password", StringTool.generateMixString(6))
                    .set("created_at", DateUtils.nowDateTime()).set("creator_id", Consts.DEFAULT_ADMINID)
                    .set("state", Consts.STATE_VALID);
            ui.save(); //此时创建的用户未启用
            UserProfile up = new UserProfile().set("user_id", ui.get("user_id")).set("gender", userData.get("gender"))
                    .set("avatar_url",userData.get("avatarUrl")).set("nick_name", userData.get("nickName")).set("sign_text", "")
                    .set("mobile_phone", "").set("user_point", 0.0f).set("level", 1);
            up.save();
            wu = new WxaUser().set("open_id", openid).set("user_id", userId).set("nick_name", userData.get("nickName"))
                    .set("gender", userData.get("gender")).set("avatar_url", userData.get("avatarUrl"))
                    .set("country", userData.get("country")).set("city", userData.get("city"))
                    .set("created_at", DateUtils.nowDateTime()).set("state", Consts.STATE_VALID);
            wu.save();
        }else if(null != userData){ //更新微信用户信息
            System.out.println("=========更新wx用户信息=========");
            wu.set("nick_name", userData.get("nickName")).set("gender", userData.get("gender")).set("avatar_url", userData.get("avatarUrl"))
                    .set("country", userData.get("country")).set("city", userData.get("city"))
                    .set("updated_at", DateUtils.nowDateTime());
            wu.update();
        }
        String sql = SqlKit.sql("user.getAccountInfo") + " where wxa.open_id=?";
        Record ui = Db.findFirst(sql, openid);
        ui.set("roleInfo", RoleInfo.dao.findById(ui.getInt("role_id")));
        UserDao userDao = JSONObject.parseObject(ui.toJson(), UserDao.class);
        return userDao;
    }
}

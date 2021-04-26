package com.sagacity.docs.service;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.sagacity.docs.base.exception.IncorrectCredentialException;
import com.sagacity.docs.base.exception.IncorrectRoleException;
import com.sagacity.docs.base.exception.LockedAccountException;
import com.sagacity.docs.base.exception.UnknownAccountException;
import com.sagacity.docs.base.extend.Consts;
import com.sagacity.docs.base.extend.RoleType;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.user.RoleInfo;
import com.sagacity.docs.model.user.UserInfo;

public class AuthRealm {

    /**
     * 管理端认证登陆方法
     * @param account
     * @param password
     */
    public UserDao doWebAuth(String account, String password) {

        UserInfo user = UserInfo.dao.findByName(account);
        if (null == user) {
            throw new UnknownAccountException("用户名不存在!");
        }
        if(user.getInt(("role_id")) == RoleType.USER) {
           throw new IncorrectRoleException("你还未成为创作者，请用小程序扫码登陆！");
        }
        if (user.getInt("state") == Consts.STATE_INVALID) {
            throw new LockedAccountException("用户被锁定!");
        }
        if (!password.equals(user.getStr("password"))){
            throw  new IncorrectCredentialException("账号或密码错误!");
        }

        Record ui = Db.findFirst(
                SqlKit.sql("user.getAccountInfo") + " where u.user_id=?", user.getStr("user_id"));
        ui.set("roleInfo", RoleInfo.dao.findById(ui.getInt("role_id")));
        UserDao userDao = JSONObject.parseObject(ui.toJson(), UserDao.class);
        //返回登陆用户对象
        return userDao;
    }
}

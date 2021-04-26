package com.sagacity.docs.web.controller;


import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Duang;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.HashKit;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;
import com.sagacity.docs.base.exception.*;
import com.sagacity.docs.base.extend.CacheKey;
import com.sagacity.docs.service.AuthRealm;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.user.UserInfo;
import com.sagacity.docs.web.common.LoginValidator;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.web.common.WebLoginInterceptor;
import com.sagacity.utility.DateUtils;

import java.util.List;

/**
 * @类名字：CommonController
 * @类描述：
 * @author:Carl.Wu
 * @版本信息：
 * @日期：2013-11-14
 * @Copyright 足下 Corporation 2013 
 * @版权所有
 *
 */

@ControllerBind(controllerKey = "/admin", viewPath = "/admin")
public class AdminController extends WebBaseController {

    @Override
    public void index(){
        UserDao userInfo = getCurrentUser();
        setAttr("userInfo", userInfo);
//        setAttr("resourceUrl", PropertiesFactoryHelper.getInstance()
//                .getConfig("resource.url"));
        //加载一级模块
        setAttr("menuList", Db.find(SqlKit.sql("sys.getModule")
                , userInfo.getRoleInfo().getRole_id()));
        setAttr("version", PropKit.get("version.number"));
        render("index.html");
    }

    public void logout() {
        String token = this.getCookie("token");
        CacheKit.remove(CacheKey.CACHE_WEB, token);
        redirect("/login.html");
    }

    /**
     * 管理用户登陆，下一步加入短信验证
     */
    @Before(LoginValidator.class)
    @Clear(WebLoginInterceptor.class)
    public void login() {
        String account = getPara("account");
        String password = getPara("password");
        String addr = getRequest().getRemoteAddr();
        String token = HashKit.md5(DateUtils.getLongDateMilliSecond()+"");

        try{
            AuthRealm au = Duang.duang(AuthRealm.class);
            UserDao user = au.doWebAuth(account, password);
            //写入缓存
            JSONObject jo = new JSONObject();
            jo.put("loginTime", DateUtils.nowDateTime());
            jo.put("userInfo", user);
            jo.put("addr", addr); //保存IP地址
            CacheKit.put(CacheKey.CACHE_WEB, token, jo);
            //写入cookie
            this.setCookie("token", token, 86400000);
            rest.success("验证成功，即将登陆!");

        }catch (UnknownAccountException une) {
            rest.error(une.getMessage());
        } catch (LockedAccountException lae) {
            rest.error(lae.getMessage());
        } catch (IncorrectCredentialException ine) {
            rest.error(ine.getMessage());
        } catch (IncorrectRoleException rl) {
            rest.error(rl.getMessage());
        } catch (AuthenticationException e) {
            rest.error("认证异常，请稍后重试!");
        }
        renderJson(rest);
    }

    @Clear(WebLoginInterceptor.class)
    public void scanLogin(){
        boolean r = false;

        //从缓存中获取传入的key,找到对应用户
        if (CacheKit.get(CacheKey.CACHE_ACCOUNT_SCAN, getPara("key")) != null){
            UserInfo userInfo = UserInfo.dao.findById(CacheKit.get(CacheKey.CACHE_ACCOUNT_SCAN, getPara("key")));
            if (userInfo != null){
                r = true;
                rest.success("验证成功，即将登陆！").setData(userInfo);
            }else{
                rest.error("用户信息错误，请联系管理员！");
            }
        }else{
            rest.error("验证失败，请重扫码！");
        }
        renderJson(rest);
    }

    /**
     * 根据Code加载下级菜单
     */
    public void loadMenuTree(){
        String moduleId = getPara("moduleId");
        UserDao userInfo = getCurrentUser();
        int roleId = userInfo.getRoleInfo().getRole_id();
        String menuSql = SqlKit.sql("sys.getMenu");
        List<Record> menuSet = Db.find(menuSql, moduleId, roleId);
        for (Record menu : menuSet){
            List<Record> subMenu = Db.find(menuSql, menu.get("id"), roleId);
            if(subMenu != null){
                menu.set("children", subMenu);
            }
        }
        renderJson(menuSet);
    }

}

package com.sagacity.docs.common;


import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;
import com.sagacity.docs.doc.DocInfo;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.extend.RoleType;
import com.sagacity.docs.user.UserInfo;
import com.sagacity.docs.utility.DateUtils;
import com.sagacity.docs.utility.PropertiesFactoryHelper;
import net.sf.json.JSONObject;

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

@ControllerBind(controllerKey = "/")
public class SystemController extends WebBaseController {

    public void index(){
        JSONObject userInfo = getCurrentUser();
        setAttr("userInfo", userInfo);
        setAttr("resourceUrl", PropertiesFactoryHelper.getInstance()
                .getConfig("resource.url"));
        //加载一级模块
        setAttr("menuList", Db.find(SqlKit.sql("sys.getModuleList"), userInfo.get("RoleID")));
        render("index.html");
    }

    public void logout() {
        String uid = this.getCookie("u_id");
        CacheKit.remove(cacheName, uid);
        redirect("/login.html");
    }

    /**
     * 管理用户登陆，下一步加入短信验证
     */
    @Before(LoginValidator.class)
    @Clear(WebLoginInterceptor.class)
    public void login() {
        boolean r = false;
        String username = getPara("username");
        String password = getPara("password");
        StringBuffer msg = new StringBuffer();

        Record user = Db.findFirst(SqlKit.sql("user.userLoginIdentify"), username, password);
        if (user != null && (user.getInt("RoleID") == RoleType.Author
                || user.getInt("RoleID") == RoleType.ADMIN)) {
            String uid = "u_" + user.getStr("UserID");
            JSONObject jo = new JSONObject();
            jo.put("AddTime", DateUtils.nowDateTime());
            jo.put("UserInfo", user.toJson());
            jo.put("DeviceInfo", getRequest().getRemoteAddr()); //保存IP地址
            CacheKit.put(cacheName, uid, jo);
            this.setCookie("u_id", uid, 86400000);
            r = true;
            msg.append("验证成功，即将登陆！");
        }else{
            Record u = Db.findFirst("select u.Caption,u.Password,u.intState from sys_users u where u.LoginName=?", username);
            if(u == null){
                msg.append("用户账号不存在！");
            }else if(u.getInt("intState") == 0){
                msg.append("用户账号已停用！");
            }else if(!u.getStr("Password").equals(password)){
                msg.append("密码错误！");
            }else{
                msg.append("登录验证错误！");
            }
        }
        responseData.put(ResponseCode.RESULT, r);
        responseData.put(ResponseCode.MSG, msg.toString());
        renderJson(responseData);
    }

    private String AccountScanCacheName = "AccountScanCacheName";

    @Clear(WebLoginInterceptor.class)
    public void scanLogin(){
        boolean r = false;

        //从缓存中获取传入的key,找到对应用户
        if (CacheKit.get(AccountScanCacheName, getPara("key")) != null){
            UserInfo userInfo = UserInfo.dao.findById(CacheKit.get(AccountScanCacheName, getPara("key")));
            if (userInfo != null){
                r = true;
                responseData.put("user", userInfo);
                responseData.put(ResponseCode.MSG, "验证成功，即将登陆！");
            }else{
                responseData.put(ResponseCode.MSG, "用户信息错误，请联系管理员！");
            }
        }else{
            responseData.put(ResponseCode.MSG, "验证失败，请重扫码！");
        }
        responseData.put(ResponseCode.RESULT, r);
        renderJson(responseData);

    }

    /**
     * 根据Code加载下级菜单
     */
    public void loadMenuTree(){
        String moduleID = getPara("moduleID");
        JSONObject userInfo = getCurrentUser();
        String menuStr = "select mf.FuncID id,mf.FuncName title,mf.FuncCSS icon,mf.FuncURL href,true spread from sys_function mf \n" +
                "inner join sys_roleFunc rf on rf.FuncID = mf.FuncID\n" +
                "where mf.FuncType='menu' and mf.intState=1 and mf.PID =? and rf.RoleID=?\n" +
                "order by mf.FuncCode";
        List<Record> menuSet = Db.find(menuStr, moduleID, userInfo.get("RoleID"));
        for (Record menu : menuSet){
            List<Record> subMenu = Db.find(menuStr, menu.get("id"),userInfo.get("RoleID"));
            if(subMenu != null){
                menu.set("children", subMenu);
            }
        }
        renderJson(menuSet);
    }

}

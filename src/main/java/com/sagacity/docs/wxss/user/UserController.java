package com.sagacity.docs.wxss.user;

import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.video.VideoInfo;
import com.sagacity.docs.weixin.UserFavor;
import com.sagacity.docs.weixin.UserFollow;
import com.sagacity.docs.weixin.UserLike;
import com.sagacity.docs.weixin.WXUser;
import com.sagacity.docs.wxss.WXSSBaseController;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.render.JsonRender;
import com.jfinal.upload.UploadFile;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;
import freemarker.template.utility.DateUtil;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mulaliu on 16/4/15.
 */
@ControllerBind(controllerKey = "/wxss/user", viewPath = "/wxss")
public class UserController extends WXSSBaseController {


    @Override
    public void index() {

    }

    /**
     * 用户账户
     */
    public void getAccountInfo(){
        int code = 1;

        String token = getPara("token");
        List<Record> dl= Db.find(SqlKit.sql("account.getFavorDoc"), token);
        List<Record> dp= Db.find(SqlKit.sql("account.getFavorPage"), token);

        responseData.put(ResponseCode.CODE, code);
        responseData.put("user", WXUser.dao.findFirst("select * from wx_user where open_id=?", token));
        //各项业务数据
        Map<String,Object> user_data = new HashMap<String, Object>();
        user_data.put("scan_code_title", "登录网页版");
        user_data.put("docCount", Db.findFirst("select count(di.id) count \n" +
                "from doc_info di\n" +
                "left join sys_users u on u.UserID=di.user_id\n" +
                "where u.OpenID=? ", token).get("count"));
        user_data.put("courseCount", Db.findFirst("select count(ci.id) count \n" +
                "from course_info ci\n" +
                "left join sys_users u on u.UserID=ci.user_id\n" +
                "where u.OpenID=? ", token).get("count"));
        user_data.put("doc", dl);
        user_data.put("doc_page", dp);
        responseData.put("user_data", user_data);

        renderJson(responseData);
    }

    /*
        添加收藏
     */
    @Before(Tx.class)
    public void userFavor(){
        boolean r = false;
        String token = getPara("token");
        int data_id = getParaToInt("data_id");
        String type = getPara("type");

        if(Db.findFirst("select * from user_favor where open_id=? and data_id=? and type=?", token, data_id, type) == null){
            r = new UserFavor().set("open_id", token).set("data_id", data_id).set("type", type)
                    .set("created_at", DateUtils.nowDateTime()).save();
            if(r){
                responseData.put("is_favor", true);
                responseData.put(ResponseCode.MSG, "收藏成功！");
            }else{
                responseData.put(ResponseCode.MSG, "收藏失败！");
            }
        }else {
            r = Db.update("delete from user_favor where open_id=? and data_id=? and type=?", token, data_id, type)>0? true: false;
            if(r){
                responseData.put("is_favor", false);
                responseData.put(ResponseCode.MSG, "取消成功！");
            }else{
                responseData.put(ResponseCode.MSG, "取消失败！");
            }
        }

        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    /*
        取消收藏
    */
    @Before(Tx.class)
    public void userFavorCancel(){
        boolean r = false;
        String token = getPara("token");
        int data_id = getParaToInt("data_id");
        UserFavor uf = UserFavor.dao.findById(data_id);
        if(uf != null){
            r = uf.delete();
            if(r){
                responseData.put(ResponseCode.MSG, "操作成功！");
            }else{
                responseData.put(ResponseCode.MSG, "操作失败！");
            }
        }else{
            responseData.put(ResponseCode.MSG, "数据异常！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    /**
     * 点赞
     */
    @Before(Tx.class)
    public void userLike(){
        boolean r = false;
        String token = getPara("token");
        int data_id = getParaToInt("data_id");
        String type = getPara("type");

        if(Db.findFirst("select * from user_like where open_id=? and data_id=? and type=?", token, data_id, type) == null){
            r = new UserLike().set("open_id", token).set("data_id", data_id).set("type", type)
                    .set("created_at", DateUtils.nowDateTime()).save();
            if(r){
                responseData.put(ResponseCode.MSG, "赞+1");
                responseData.put("likes", Db.find("select user.open_id,user.avatar_url,user.nick_name " +
                        "from user_like ul\n" +
                        "left join wx_user user on user.open_id=ul.open_id\n" +
                        "where ul.data_id=? and ul.type=?", data_id, type));
            }else{
                responseData.put(ResponseCode.MSG, "点赞失败！");
            }
        }else{
            responseData.put(ResponseCode.MSG, "你已点过赞了！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    /**
     * 订阅
     */
    @Before(Tx.class)
    public void userFollow(){
        boolean r = false;
        String token = getPara("token");
        int data_id = getParaToInt("data_id");

        VideoInfo video = VideoInfo.dao.findById(data_id);
        if(Db.findFirst("select * from user_follow where open_id=? and data_id=?", token, video.get("user_id")) == null){
            r = new UserFollow().set("open_id", token).set("data_id", video.get("user_id"))
                    .set("created_at", DateUtils.nowDateTime()).save();
            if(r){
                responseData.put(ResponseCode.MSG, "订阅成功！");
            }else{
                responseData.put(ResponseCode.MSG, "订阅失败！");
            }
        }else{
            responseData.put(ResponseCode.MSG, "你已订阅！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    public void getPayList(){
        int page = getParaToInt("page"); //数据分页
        String token = getPara("token");

        String sql_select = "select pay.*";
        String sql_from = "from (\n" +
                "select pi.pay_id,pi.cost,'doc' type,pi.data_id,di.title,pi.order_code,pi.created_at,di.user_id\n" +
                "from pay_info pi\n" +
                "left join doc_info di on di.id=pi.data_id\n" +
                "where pi.state=2 and pi.data_type='doc'\n" +
                "UNION\n" +
                "select pi.pay_id,pi.cost,'video' type,pi.data_id,vi.title,pi.order_code,pi.created_at,vi.user_id\n" +
                "from pay_info pi\n" +
                "left join video_info vi on vi.id=pi.data_id\n" +
                "where pi.state=2 and pi.data_type='video' ) pay\n"+
                "left join sys_users u on u.UserID=pay.user_id\n" +
                "where u.OpenID=?";
        sql_from += " order by pay.created_at Desc";
        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            Page<Record> dataList = Db.paginate(page, getParaToInt("pageSize", 12)
                    , sql_select, sql_from, token);
            renderJson(dataList);
        }else {
            renderJson(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from, token));
        }

    }

}

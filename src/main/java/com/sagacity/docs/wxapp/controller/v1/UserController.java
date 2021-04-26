package com.sagacity.docs.wxapp.controller.v1;

import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.video.VideoInfo;
import com.sagacity.docs.model.user.UserFavor;
import com.sagacity.docs.model.user.UserFollow;
import com.sagacity.docs.model.user.UserLike;
import com.sagacity.docs.wxapp.common.WXSSBaseController;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;

import java.util.List;

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
        UserDao userInfo = getCurrentUser(getPara("token"));

        if(userInfo != null){
            List<Record> dl= Db.find(SqlKit.sql("account.getFavorDoc"), userInfo.getUser_id());
            List<Record> dp= Db.find(SqlKit.sql("account.getFavorPage"), userInfo.getUser_id());

            data.put("user", userInfo);
            //各项业务数据
            data.put("scan_code_title", "登录网页版");
            data.put("docCount", Db.findFirst("select count(di.id) count \n" +
                    "from doc_info di\n" +
                    "where di.user_id=? ", userInfo.getUser_id()).get("count"));
            data.put("videoCount", Db.findFirst("select count(vi.id) count \n" +
                    "from video_info vi\n" +
                    "where vi.user_id=? ", userInfo.getUser_id()).get("count"));
            data.put("doc", dl);
            data.put("doc_page", dp);
            rest.success().setData(data);
        }else{
            //各项业务数据
            rest.error();
        }

        renderJson(rest);
    }

    /*
        添加收藏
     */
    @Before(Tx.class)
    public void userFavor(){
        boolean r = false;
        UserDao userInfo = getCurrentUser(getPara("token"));
        int dataId = getParaToInt("dataId");
        String type = getPara("type");

        if(Db.findFirst("select * from user_favor where user_id=? and data_id=? and type=?"
                , userInfo.getUser_id(), dataId, type) == null){
            r = new UserFavor().set("user_id", userInfo.getUser_id()).set("data_id", dataId).set("type", type)
                    .set("created_at", DateUtils.nowDateTime()).save();
            if(r){
                data.put("is_favor", true);
                rest.success("收藏成功！").setData(data);
            }else{
                rest.error("收藏失败！");
            }
        }else {
            r = Db.update("delete from user_favor where user_id=? and data_id=? and type=?"
                    , userInfo.getUser_id(), dataId, type)>0? true: false;
            if(r){
                data.put("is_favor", false);
                rest.success("取消成功！").setData(data);
            }else{
                rest.error("取消失败！");
            }
        }

        renderJson(rest);
    }

    /*
        取消收藏
    */
    @Before(Tx.class)
    public void userFavorCancel(){
        boolean r = false;
        UserDao userInfo = getCurrentUser(getPara("token"));
        int dataId = getParaToInt("dataId");
        UserFavor uf = UserFavor.dao.findById(dataId);
        if(uf != null){
            r = uf.delete();
            if(r){
                rest.success("操作成功！");
            }else{
                rest.error("操作失败！");
            }
        }else{
            rest.error("数据异常！");
        }
        renderJson(rest);
    }

    /**
     * 点赞
     */
    @Before(Tx.class)
    public void userLike(){
        boolean r = false;
        UserDao userInfo = getCurrentUser(getPara("token"));
        int dataId = getParaToInt("dataId");
        String type = getPara("type");

        if(Db.findFirst("select * from user_like where user_id=? and data_id=? and type=?"
                , userInfo.getUser_id(), dataId, type) == null){
            r = new UserLike().set("user_id", userInfo.getUser_id()).set("data_id", dataId).set("type", type)
                    .set("created_at", DateUtils.nowDateTime()).save();
            if(r){
                data.put("likes", Db.find("select u.user_id,up.nick_name,up.avatar_url " +
                        "from user_like ul\n" +
                        "left join sys_users u on u.user_id=ul.user_id\n" +
                        "left join user_profile up on up.user_id=u.user_id\n" +
                        "where ul.data_id=? and ul.type=?\n" +
                        "order by ul.created_at", dataId, type));
                rest.success("赞+1").setData(data);
            }else{
                rest.error("点赞失败！");
            }
        }else{
            rest.error("你已点过赞了！");
        }

        renderJson(rest);
    }

    /**
     * 订阅
     */
    @Before(Tx.class)
    public void userFollow(){
        boolean r = false;
        UserDao userInfo = getCurrentUser(getPara("token"));
        int dataId = getParaToInt("dataId");

        VideoInfo video = VideoInfo.dao.findById(dataId);
        if(Db.findFirst("select * from user_follow where user_id=? and data_id=?"
                , userInfo.getUser_id(), video.get("user_id")) == null){
            r = new UserFollow().set("user_id", userInfo.getUser_id()).set("data_id", video.get("user_id"))
                    .set("created_at", DateUtils.nowDateTime()).save();
            if(r){
                rest.success("订阅成功！");
            }else{
                rest.error("订阅失败！");
            }
        }else{
            rest.error("你已订阅！");
        }

        renderJson(rest);
    }

    public void getPayList(){
        int page = getParaToInt("page"); //数据分页
        UserDao userInfo = getCurrentUser(getPara("token"));

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
                "left join sys_users u on u.user_id=pay.user_id\n" +
                "where u.user_id=?";
        sql_from += " order by pay.created_at Desc";
        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            Page<Record> dataList = Db.paginate(page, getParaToInt("pageSize", 12)
                    , sql_select, sql_from, userInfo.getUser_id());
            rest.success().setData(dataList);
        }else {
            data.put(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from, userInfo.getUser_id()));
            rest.success().setData(data);
        }
        renderJson(rest);

    }

}

package com.sagacity.docs.wxss.video;

import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.qiniu.http.Response;
import com.sagacity.docs.common.WebBaseController;
import com.sagacity.docs.utility.ColorUtil;
import com.sagacity.docs.video.VideoInfo;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.utility.StringTool;
import com.sagacity.docs.weixin.WXUser;
import com.sagacity.docs.wxss.WXSSBaseController;

import java.util.List;

@ControllerBind(controllerKey = "/wxss/video", viewPath = "/wxss")
public class VideoController extends WXSSBaseController {

    @Override
    public void index(){

    }

    public void getVideoClass(){
        String sql = "select * from video_class";
        if(StringTool.notNull(getPara("token")) && !StringTool.isBlank(getPara("token"))){
            //传入token,判断用户权限，设置可打开内容，后续优化
            WXUser user = WXUser.dao.findFirst("select * from wx_user where open_id=?", getPara("token"));
            if (user != null){
                switch (user.getInt("level_id")){
                    case 1: //初级用户
                        sql += " where state=1";
                        break;
                    case 2: //中级用户
                        sql += " where 1=1";
                    default:
                        break;
                }
            }else{
                sql += " where state=1";
            }
        }else{
            sql += " where state=1";
        }
        if (StringTool.notNull(getPara("is_hot")) && !StringTool.isBlank(getPara("is_hot"))){
            sql += " and is_hot=" + getPara("is_hot");
        }
        renderJson(ResponseCode.LIST, Db.find(sql));
    }

    /**
     * 支持classID | tag 查询
     */
    public void getVideoList(){
        int page = getParaToInt("page"); //数据分页
        int isHot = getParaToInt("is_hot", 1);

        String sqlSelect = "select ci.id,ci.cover,ci.view_count,ci.title,ci.desc,source,is_live,u.Caption";
        String sqlFrom = "from video_info ci\n" +
                "left join sys_users u on u.UserID=ci.user_id\n"+
                "where ci.state=1 and ci.is_hot=?";
        sqlFrom += " order by ci.created_at DESC";
        //分页
        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            renderJson(Db.paginate(page,  getParaToInt("pageSize", 12),
                    sqlSelect, sqlFrom, isHot));
        }else{
            renderJson(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom, isHot));
        }
    }

    public void getClassVideoList(){
        int page = getParaToInt("page"); //数据分页
        int class_id = getParaToInt("class_id", 0);

        String sqlSelect = "select ci.id,ci.cover,ci.view_count,ci.title,ci.desc,source,is_live,u.Caption";
        String sqlFrom = "from video_info ci\n" +
                "left join sys_users u on u.UserID=ci.user_id\n"+
                "where ci.state=1";
        if(class_id == -1){//特殊处理
            sqlFrom += " and ci.view_count>200 order by ci.view_count DESC";
        }else{
            sqlFrom += " and ci.video_class_id="+class_id+" order by ci.created_at DESC";
        }
        //分页
        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            renderJson(Db.paginate(page,  getParaToInt("pageSize", 12),
                    sqlSelect, sqlFrom));
        }else{
            renderJson(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom));
        }
    }

    /**
     * 获得相关视频
     */
    public void getRelatedVideoList(){
        int page = getParaToInt("page"); //数据分页
        int videoID = getParaToInt("video_id");

        String sqlSelect = "select ci.id,ci.cover,ci.title,ci.desc,ci.source,ci.is_live\n";
        String sqlFrom = "from video_info ci\n" +
                "left join video_info cl on cl.video_class_id=ci.video_class_id\n" +
                "where ci.state=1 and cl.id=? and ci.id != ?";

        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            renderJson(Db.paginate(page,
                    getParaToInt("pageSize", 10), sqlSelect, sqlFrom, videoID, videoID));
        }else {
            renderJson(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom, videoID, videoID));
        }
    }

    public void getVideoInfo(){
        int videoID = getParaToInt("video_id");

        Record video = Db.findFirst(SqlKit.sql("video.getVideoInfo"), videoID);
        //弹幕
        String sql = "select content text, time from danmu where video_id=0";
        List<Record> dl = Db.find(sql);
        for (Record d : dl){
            d.set("color", ColorUtil.getColorRandom());
        }
        responseData.put("danmus", dl);

        Db.update("update video_info set view_count=view_count+1 where id=?", videoID);
        //是否已订阅频道
        video.set("is_follow", false);
        if(StringTool.notNull(getPara("token"))){
            if(Db.findFirst("select * from user_follow where open_id=? and data_id=?", getPara("token"), video.get("user_id")) != null){
                video.set("is_follow", true);
            }
        }
        responseData.put("video", video);
        //点赞人员列表
        responseData.put("likes", Db.find("select user.open_id,user.avatar_url,user.nick_name " +
                "from user_like ul\n" +
                "left join wx_user user on user.open_id=ul.open_id\n" +
                "where ul.data_id=? and ul.type=?\n" +
                "order by ul.created_at DESC", videoID, "video"));
        //相关视频
        String sqlRC = "select ci.id,ci.cover,ci.title,ci.desc,ci.view_count,ci.source,ci.is_live,u.Caption\n" +
                "from video_info ci\n" +
                "left join video_info cl on cl.video_class_id=ci.video_class_id\n" +
                "left join sys_users u on u.UserID=ci.user_id\n"+
                "where ci.state=1 and cl.id=? and ci.id != ?\n" +
                "limit 6";
        responseData.put("relatedVideos", Db.find(sqlRC, videoID, videoID));
        renderJson(responseData);
    }

    /**
     * 订阅列表
     */
    public void getSubscribeList(){
        String token = getPara("token");
    }
}

package com.sagacity.docs.wxapp.controller.v1;

import com.jfinal.aop.Clear;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.wxapp.common.WXSSBaseController;
import com.sagacity.docs.wxapp.common.WXSSLoginInterceptor;
import com.sagacity.utility.ColorUtil;
import com.sagacity.utility.StringTool;

import java.util.List;

@ControllerBind(controllerKey = "/wxss/video", viewPath = "/wxss")
public class VideoController extends WXSSBaseController {

    @Override
    public void index(){

    }

    public void getVideoClass(){
        UserDao userInfo = getCurrentUser(getPara("token"));

        String sql = "select * from video_class";
        if(userInfo != null){
            //判断用户级别，设置可打开内容，后续优化
            switch (userInfo.getLevel()){
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
        if (StringTool.notNull(getPara("is_hot")) && !StringTool.isBlank(getPara("is_hot"))){
            sql += " and is_hot=" + getPara("is_hot");
        }
        data.put(ResponseCode.LIST, Db.find(sql));
        rest.success().setData(data);
        renderJson(data);
    }

    /**
     * 支持classID | tag 查询
     */
    public void getVideoList(){
        int page = getParaToInt("page"); //数据分页
        int isHot = getParaToInt("isHot", 1);

        String sqlSelect = "select ci.id,ci.cover,ci.view_count,ci.title,ci.desc,source,is_live,u.caption";
        String sqlFrom = "from video_info ci\n" +
                "left join sys_users u on u.user_id=ci.user_id\n"+
                "where ci.state=1 and ci.is_hot=?";
        sqlFrom += " order by ci.created_at DESC";
        //分页
        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            Page<Record> videoList = Db.paginate(page,  getParaToInt("pageSize", 12),
                    sqlSelect, sqlFrom, isHot);
            rest.success().setData(videoList);
        }else{
            data.put(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom, isHot));
            rest.success().setData(data);
        }
        renderJson(rest);
    }

    /**
     * 获得分类或热门或指定人的视频列表
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getClassVideoList() {
        int page = getParaToInt("page"); //数据分页
        int classId = getParaToInt("classId", 0);

        String sqlSelect = "select ci.id,ci.cover,ci.view_count,ci.title,ci.desc,source,is_live,up.nick_name";
        String sqlFrom = "from video_info ci\n" +
                "left join sys_users u on u.user_id=ci.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where ci.state=1";
        //按作者过滤
        if (StringTool.notNull(getPara("userId")) && StringTool.notBlank(getPara("userId"))) {
            sqlFrom += " and ci.user_id='" + getPara("userId") + "'";
        }
        if (classId == -1) {//特殊处理
            sqlFrom += " and ci.view_count>300 order by ci.view_count DESC";
        } else if (classId == 0){
            sqlFrom += " order by ci.view_count DESC";
        } else{
            sqlFrom += " and ci.video_class_id="+classId+" order by ci.created_at DESC";
        }
        //分页
        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            Page<Record> videoList =  Db.paginate(page,  getParaToInt("pageSize", 12),
                    sqlSelect, sqlFrom);
            rest.success().setData(videoList);
        }else{
            data.put(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom));
            rest.success().setData(data);
        }
        renderJson(rest);
    }

    /**
     * 获得相关视频
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getRelatedVideoList(){
        int page = getParaToInt("page"); //数据分页
        int videoId = getParaToInt("videoId");

        String sqlSelect = "select ci.id,ci.cover,ci.title,ci.desc,ci.source,ci.is_live\n";
        String sqlFrom = "from video_info ci\n" +
                "left join video_info cl on cl.video_class_id=ci.video_class_id\n" +
                "where ci.state=1 and cl.id=? and ci.id != ?";

        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            Page<Record> videoList = Db.paginate(page,
                    getParaToInt("pageSize", 10), sqlSelect, sqlFrom, videoId, videoId);
            rest.success().setData(videoList);
        }else {
            data.put(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom, videoId, videoId));
            rest.success().setData(data);
        }
        renderJson(rest);
    }

    @Clear(WXSSLoginInterceptor.class)
    public void getVideoInfo(){
        UserDao userInfo = getCurrentUser(getPara("token"));
        int videoId = getParaToInt("videoId");

        Record video = Db.findFirst(SqlKit.sql("video.getVideoInfo"), videoId);
        //弹幕
        String sql = "select content text, time from danmu where video_id=?";
        List<Record> dl = Db.find(sql, videoId);
        for (Record d : dl){
            d.set("color", ColorUtil.getColorRandom());
        }
        data.put("danmus", dl);
        //分集
        String sql2 = "select ed.id,ed.episode_title,ed.source_url from video_episode ed where ed.state=1 and ed.video_id=? order by ed.order";
        data.put("episodes", Db.find(sql2, videoId));
        //更新查看次数
        Db.update("update video_info set view_count=view_count+1 where id=?", videoId);
        //是否已订阅频道
        data.put("is_follow", false);
        if(userInfo != null){
            if(Db.findFirst("select * from user_follow where user_id=? and data_id=?", userInfo.getUser_id(), video.get("user_id")) != null){
                data.put("is_follow", true);
            }
        }
        //收藏
        data.put("is_favor", false);
        if(userInfo != null){
            if(Db.findFirst("select * from user_favor where user_id=? and data_id=? and type=?", userInfo.getUser_id(), videoId, "video") != null){
                data.put("is_favor", true);
            }
        }
        data.put("video", video);
        //点赞人员列表
        data.put("likes", Db.find("select u.user_id,up.avatar_url,up.nick_name " +
                "from user_like ul\n" +
                "left join sys_users u on u.user_id=ul.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where ul.data_id=? and ul.type=?\n" +
                "order by ul.created_at DESC", videoId, "video"));
        //相关视频
        String sqlRC = "select ci.id,ci.cover,ci.title,ci.desc,ci.view_count,ci.source,ci.is_live,up.nick_name\n" +
                "from video_info ci\n" +
                "left join video_info cl on cl.video_class_id=ci.video_class_id\n" +
                "left join sys_users u on u.user_id=ci.user_id\n"+
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where ci.state=1 and cl.id=? and ci.id != ?\n" +
                "order by ci.view_count DESC \n"+
                "limit 6";
        data.put("relatedVideos", Db.find(sqlRC, videoId, videoId));
        rest.success().setData(data);
        renderJson(rest);
    }

    /**
     * 订阅列表
     */
    public void getSubscribeList(){
        UserDao userInfo = getCurrentUser(getPara("token"));
    }
}

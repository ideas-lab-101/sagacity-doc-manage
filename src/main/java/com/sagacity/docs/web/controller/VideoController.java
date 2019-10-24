package com.sagacity.docs.web.controller;

import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.upload.UploadFile;
import com.sagacity.docs.model.video.VideoEpisode;
import com.sagacity.docs.model.video.VideoInfo;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.base.extend.RoleType;
import com.sagacity.docs.base.openapi.Qiniu;
import com.sagacity.utility.ConvertUtil;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;
import net.sf.json.JSONObject;

import java.io.File;
import java.util.List;

@ControllerBind(controllerKey = "/admin/video", viewPath = "/admin/video")
public class VideoController extends WebBaseController{

    @Before(Tx.class)
    public void initVideoEpisode(){
        boolean r = false;
        List<VideoInfo> vl = VideoInfo.dao.find("select * from video_info");
        for(VideoInfo v : vl){
            VideoEpisode ve = new VideoEpisode().set("video_id", v.get("id"))
                    .set("episode_title", "01").set("source_url", v.get("source_url"))
                    .set("created_at", DateUtils.nowDateTime()).set("view_count",0)
                    .set("state", 1);
            r = ve.save();
            r = ve.set("order", ve.get("id")).update();
        }
        if(r){
            responseData.put(ResponseCode.MSG, "初始化成功！");
        }else{
            responseData.put(ResponseCode.MSG, "初始化失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);

    }

    @Override
    public void index(){

    }

    public void getVideoList(){
        JSONObject userInfo = getCurrentUser();

        String sqlSelect = SqlKit.sql("video.getVideoList-select");
        String sqlFrom = SqlKit.sql("video.getVideoList-from");
        if(userInfo.getInt("RoleID") == RoleType.ADMIN){
            sqlFrom += " where 1=1";
        }else{
            sqlFrom += " where video.user_id='"+userInfo.get("UserID")+"'";
        }
        if(StringTool.notNull(getPara("key")) && StringTool.notBlank(getPara("key"))){
            sqlFrom += " and video.title like '%"+getPara("key")+"%'";
        }
        sqlFrom += " order by video.created_at Desc";
        if (StringTool.notNull(getPara("pageIndex")) && !StringTool.isBlank(getPara("pageIndex"))){
            Page<Record> noticeList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
            renderJson(convertPageData(noticeList));
        }else {
            renderJson(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom));
        }
    }

    public void editVideo(){
        int video_id = getParaToInt("video_id");
        String sql = SqlKit.sql("video.getVideoInfo");
        setAttr("video", Db.findFirst(sql, video_id));
        render("editVideo.html");
    }

    /**
     * 获得视频集
     */
    public void getEpisodeList(){
        int video_id = getParaToInt("video_id");

        String sql = "select ed.* from video_episode ed where video_id=? order by ed.order";
        responseData.put(ResponseCode.CODE, 0);
        responseData.put(ResponseCode.DATA, Db.find(sql, video_id));
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void setEpisodeState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = VideoEpisode.dao.findById(getPara("id")).set("state", state)
                .set("updated_at", DateUtils.nowDateTime()).update();
        if(r){
            responseData.put(ResponseCode.MSG, "设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "设置失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void addEpisode(){
        boolean r = false;
        JSONObject userInfo = getCurrentUser();

        VideoEpisode ve = new VideoEpisode().set("video_id",getPara("video_id"))
                .set("episode_title", "新分集").set("view_count",0).set("state", 1).set("created_at", DateUtils.nowDateTime());
        r = ve.save();
        r = ve.set("order", ve.get("id")).update();
        if (r) {
            responseData.put(ResponseCode.MSG, "新增成功！");
        }else{
            responseData.put(ResponseCode.MSG, "新增失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void delEpisode(){
        boolean r = false;
        JSONObject jo = getCurrentUser();
        VideoEpisode m =  VideoEpisode.dao.findById(getParaToInt("id"));
        r = m.delete();
        if(r){
            responseData.put(ResponseCode.MSG, "删除成功！");
        }else{
            responseData.put(ResponseCode.MSG, "删除失败！");
        }

        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void saveEpisode(){
        Record data = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("data")));

        boolean r = Db.update("video_episode", data.set("updated_at", DateUtils.nowDateTime()));
        if(r){
            responseData.put(ResponseCode.MSG, "修改成功！");
        }else{
            responseData.put(ResponseCode.MSG, "修改失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void saveVideo(){
        boolean r = getModel(VideoInfo.class, "video")
                .set("updated_at", DateUtils.nowDateTime()).update();
        if(r){
            responseData.put(ResponseCode.MSG, "视频更新成功！");
        }else{
            responseData.put(ResponseCode.MSG, "视频更新失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void addVideo(){
        JSONObject userInfo = getCurrentUser();
        VideoInfo video = getModel(VideoInfo.class, "video");
        boolean r = video.set("user_id", userInfo.get("UserID")).set("state",1).set("cover","/assets/images/video_cover.png")
                .set("view_count",0).set("order",1).set("is_hot",0).set("is_ad", 0).set("created_at", DateUtils.nowDateTime()).save();
        if(r){
            responseData.put(ResponseCode.MSG, "视频新增成功！");
        }else{
            responseData.put(ResponseCode.MSG, "视频新增失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void delVideo(){
        boolean r = false;
        int video_id= getParaToInt("video_id");
        if( 1 == 2){
            responseData.put(ResponseCode.MSG, "不允许删除！");
        }else{
            r = VideoInfo.dao.deleteById(video_id);
            if(r){
                responseData.put(ResponseCode.MSG, "视频删除成功！");
            }else{
                responseData.put(ResponseCode.MSG, "视频删除失败！");
            }
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void uploadCover(){
        String filePath = "";
        String upToken = Qiniu.dao.getUploadToken(); //7牛上传token
        String qiniu_url = PropKit.get("qiniu.url");
        String config_dir = PropKit.get("resource.dir");
        boolean r= true;

        File f1 = new File(config_dir+"/imgTemp/");
        if (!f1.exists()) {
            f1.mkdirs();
        }
        UploadFile uploadFile = getFile("coverFile", f1.getAbsolutePath());
        File nFile = uploadFile.getFile();
        if (nFile!=null) {
            //向7牛上传
            filePath = Qiniu.dao.uploadFile(nFile, upToken, "video/cover/");
            if(filePath != ""){
                r = VideoInfo.dao.findById(getPara("video_id")).set("cover", qiniu_url+filePath).update();
            }else{
                r = false;
            }
        }else if (nFile==null){
            r = false;
        }
        nFile.delete();
        if (r) {
            responseData.put("url", qiniu_url+filePath);
            responseData.put(ResponseCode.MSG, "cover设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "cover设置失败！");
        }
        responseData.put(ResponseCode.CODE, r?1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void setProperty(){
        boolean r = false;
        int property = getParaToBoolean("property")? 1:0;
        String type = getPara("type");
        switch (type){
            case "state" :
                r = VideoInfo.dao.findById(getPara("video_id")).set("state", property).update();
                break;
            case "hot" :
                r = VideoInfo.dao.findById(getPara("video_id")).set("is_hot", property).update();
                break;
            case "ad" :
                r = VideoInfo.dao.findById(getPara("video_id")).set("is_ad", property).update();
                break;
            default:
                break;
        }
        if(r){
            responseData.put(ResponseCode.MSG, "操作成功！");
        }else{
            responseData.put(ResponseCode.MSG, "操作失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

//    @Before(Tx.class)
//    public void setState(){
//        boolean r = false;
//        int state = getParaToBoolean("state")? 1:0;
//        r = VideoInfo.dao.findById(getPara("video_id")).set("state", state).update();
//        if(r){
//            responseData.put(ResponseCode.MSG, "设置成功！");
//        }else{
//            responseData.put(ResponseCode.MSG, "设置失败！");
//        }
//        responseData.put(ResponseCode.CODE, r? 1:0);
//        renderJson(responseData);
//    }
//
//    @Before(Tx.class)
//    public void setHot(){
//        boolean r = false;
//        int is_hot = getParaToBoolean("is_hot")? 1:0;
//        r = VideoInfo.dao.findById(getPara("video_id")).set("is_hot", is_hot).update();
//        if(r){
//            responseData.put(ResponseCode.MSG, "设置成功！");
//        }else{
//            responseData.put(ResponseCode.MSG, "设置失败！");
//        }
//        responseData.put(ResponseCode.CODE, r? 1:0);
//        renderJson(responseData);
//    }
//
//    @Before(Tx.class)
//    public void setAd(){
//        boolean r = false;
//        int is_ad = getParaToBoolean("is_ad")? 1:0;
//        r = VideoInfo.dao.findById(getPara("video_id")).set("is_ad", is_ad).update();
//        if(r){
//            responseData.put(ResponseCode.MSG, "设置成功！");
//        }else{
//            responseData.put(ResponseCode.MSG, "设置失败！");
//        }
//        responseData.put(ResponseCode.CODE, r? 1:0);
//        renderJson(responseData);
//    }

}

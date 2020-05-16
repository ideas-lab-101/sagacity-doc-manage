package com.sagacity.docs.web.controller;

import com.jfinal.aop.Before;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.*;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.upload.UploadFile;
import com.sagacity.docs.model.system.DocClass;
import com.sagacity.docs.model.system.MainInfo;
import com.sagacity.docs.model.system.Music;
import com.sagacity.docs.model.system.VideoClass;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.service.Qiniu;
import com.sagacity.utility.ConvertUtil;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;
import net.sf.json.JSONObject;

import java.io.File;
import java.util.List;

@ControllerBind(controllerKey = "/admin/system", viewPath = "/admin/system")
public class SystemController extends WebBaseController{

    @Override
    public void index(){

    }

    /**
     * 获得树形结构数据
     */
    public void getClassTree(){
        int treeLevel = 1;

        String sql = "select dc.id,dc.title,dc.order,dc.parent_id,true spread\n" +
                "from doc_class dc\n" +
                "where dc.parent_id=?\n" +
                "order by dc.order DESC";
        List<Record> ms = Db.find(sql, 0);
        addSubMenu(ms, treeLevel);
        renderJson(ResponseCode.DATA, ms);
    }

    private void addSubMenu(List<Record> ms, int tl){
        tl ++;
        String sql = "select dc.id,dc.title,dc.order,dc.parent_id,true spread\n" +
                "from doc_class dc\n" +
                "where dc.parent_id=?\n" +
                "order by dc.order DESC";
        for(Record m : ms){ //循环找到下级
            List<Record> subs = Db.find(sql, m.getInt("id"));
            m.set("children", subs);
            if(tl > 2){ //控制展开的层级
                m.set("spread", false);
            }
            if(subs.size()>0){
                addSubMenu(subs, tl);
            }
        }
    }

    public void getClassInfo(){
        int classID = getParaToInt("id");
        renderJson(ResponseCode.DATA, DocClass.dao.findById(classID));
    }

    @Before(Tx.class)
    public void saveClass(){
        boolean r = false;
        Record form = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("formData")));

        if(form.get("id").equals("0")){ //判断新增或更新动作
            form.set("created_at", DateUtils.nowDateTime());
            r = Db.save("doc_class", form);
        }else {
            form.set("updated_at", DateUtils.nowDateTime());
            r = Db.update("doc_class", form);
        }
        if(r){
            responseData.put(ResponseCode.MSG, "目录操作成功！");
        }else{
            responseData.put(ResponseCode.MSG, "目录操作失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void delClass(){
        boolean r = false;
        int id = getParaToInt("id");
        if(Db.find("select * from doc_info where doc_class_id=?",id).size()>0){
            responseData.put(ResponseCode.MSG, "目录包含文档数据，不允许删除！");
        }else if(Db.find("select * from doc_class where parent_id=?",id).size()>0){
            responseData.put(ResponseCode.MSG, "目录包含下级目录，不允许删除！");
        }else {
            r = DocClass.dao.deleteById(id);
            if(r){
                responseData.put(ResponseCode.MSG, "目录删除成功！");
            }else{
                responseData.put(ResponseCode.MSG, "目录删除失败！");
            }
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    /**
     * 封面管理
     */
    public void getMainList(){

        String sql = "select m.id,m.title,m.name,mt.name main_type\n" +
                ",m.page,m.cover,m.state,m.cover,SUBSTR(m.created_at,1,10) create_date\n" +
                "from main m \n" +
                "left join main_type mt on mt.id=m.type_id\n" +
                "order by m.type_id\n";
        responseData.put(ResponseCode.CODE, 0);
        responseData.put(ResponseCode.DATA, Db.find(sql));
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void editMain(){

        Record data = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("data")))
                .set("updated_at", DateUtils.nowDateTime()).remove("main_type").remove("create_date");
        boolean r = Db.update("main", data);
        if(r){
            responseData.put(ResponseCode.MSG, "更新成功！");
        }else{
            responseData.put(ResponseCode.MSG, "更新失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void setMainState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update main set state=? where id=?", state, getPara("main_id"))>0? true:false;
        if(r){
            responseData.put(ResponseCode.MSG, "设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "设置失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void uploadMainCover(){
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
            filePath = Qiniu.dao.uploadFile(nFile, upToken, "main/cover/");
            if(filePath != ""){
                r = MainInfo.dao.findById(getPara("main_id")).set("cover", qiniu_url+filePath).update();
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


    /**
     * 视频分类标签
     */
    public void getTagList(){
        String sql = "select tag.id,tag.title,tag.desc,tag.css,tag.is_hot,tag.state from video_class tag\n" +
                "where 1=1";
        responseData.put(ResponseCode.CODE, 0);
        responseData.put(ResponseCode.DATA, Db.find(sql));
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void setTagState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update video_class set state=? where id=?", state, getPara("tag_id"))>0? true:false;
        if(r){
            responseData.put(ResponseCode.MSG, "设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "设置失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void setTagHot(){
        boolean r = false;
        int is_hot = getParaToBoolean("is_hot")? 1:0;
        r = Db.update("update video_class set is_hot=? where id=?", is_hot, getPara("tag_id"))>0? true:false;
        if(r){
            responseData.put(ResponseCode.MSG, "设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "设置失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void editTag(){

        Record data = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("data")));
        if (data.get("css").equals("null")){ //样式字段可选
            data.remove("css");
        }
        boolean r = Db.update("video_class", data);
        if(r){
            responseData.put(ResponseCode.MSG, "更新成功！");
        }else{
            responseData.put(ResponseCode.MSG, "更新失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void addTag(){
        boolean r = false;

        r = new VideoClass().set("title", getPara("name"))
                .set("order", 1).set("created_at", DateUtils.nowDateTime())
                .set("is_hot", 0).set("state", 1).save();
        if(r){
            responseData.put(ResponseCode.MSG, "新增成功！");
        }else{
            responseData.put(ResponseCode.MSG, "新增失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void delTag(){
        boolean r = false;
        int id = getParaToInt("tag_id");
        if(Db.find("select * from video_info where video_class_id=?",id).size()>0){
            responseData.put(ResponseCode.MSG, "分类包含视频数据，不允许删除！");
        }else {
            r = VideoClass.dao.deleteById(id);
            if(r){
                responseData.put(ResponseCode.MSG, "分类删除成功！");
            }else{
                responseData.put(ResponseCode.MSG, "分类删除失败！");
            }
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    /**
     * 音乐库
     */

    public void getMusicList(){
        String sql_select = "select id,title,cover_url,resource_url,state,created_at,u.Caption\n";
        String sql_from = "from music m\n" +
                "left join sys_users u on u.UserID=m.user_id\n" +
                "where 1=1";

        if (StringTool.notNull(getPara("state")) && !StringTool.isBlank(getPara("state"))){
            sql_from += " and m.state="+ getPara("state");
        }
        sql_from += " order by created_at DESC";
        if (StringTool.notNull(getPara("pageIndex")) && !StringTool.isBlank(getPara("pageIndex"))){
            Page<Record> noticeList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sql_select, sql_from);
            renderJson(convertPageData(noticeList));
        }else {
            renderJson(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from));
        }
    }

    @Before(Tx.class)
    public void setMusicState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update music set state=? where id=?", state, getPara("music_id"))>0? true:false;
        if(r){
            responseData.put(ResponseCode.MSG, "设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "设置失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void editMusic(){

        Record data = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("data")));
        data.remove("Caption");
        boolean r = Db.update("music", data);
        if(r){
            responseData.put(ResponseCode.MSG, "更新成功！");
        }else{
            responseData.put(ResponseCode.MSG, "更新失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void addMusic(){
        boolean r = false;
        JSONObject jo = getCurrentUser();

        r = new Music().set("title", getPara("name")).set("cover_url", "/assets/images/music_cover.png")
                .set("created_at", DateUtils.nowDateTime()).set("state", 0)
                .set("user_id", jo.get("UserID")).save();
        if(r){
            responseData.put(ResponseCode.MSG, "新增成功！");
        }else{
            responseData.put(ResponseCode.MSG, "新增失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void delMusic(){
        boolean r = false;
        JSONObject jo = getCurrentUser();

        Music m =  Music.dao.findById(getParaToInt("music_id"));
        if(m.getStr("user_id").equals(jo.getString("UserID"))){
            r = m.delete();
            if(r){
                responseData.put(ResponseCode.MSG, "删除成功！");
            }else{
                responseData.put(ResponseCode.MSG, "删除失败！");
            }
        }else{
            responseData.put(ResponseCode.MSG, "非创建人不能删除！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void uploadMusicFile(){
        JSONObject jo = getCurrentUser();

        String filePath = "";
        String upToken = Qiniu.dao.getUploadToken(); //7牛上传token
        String qiniu_url = PropKit.get("qiniu.url");
        String config_dir = PropKit.get("resource.dir");
        boolean r= true;

        File f1 = new File(config_dir+"/fileTemp/");
        if (!f1.exists()) {
            f1.mkdirs();
        }
        UploadFile uploadFile = getFile("musicFile", f1.getAbsolutePath());
        File nFile = uploadFile.getFile();
        if (nFile!=null) {
            //向7牛上传
            filePath = Qiniu.dao.uploadFile(nFile, upToken, "music/");
            if(filePath != ""){
                //取文件名为默认音乐名
                r = new Music().set("title", nFile.getName()).set("cover_url", "")
                        .set("created_at", DateUtils.nowDateTime()).set("state", 0)
                        .set("user_id", jo.get("UserID")).set("resource_url", qiniu_url+filePath).save();
            }else{
                r = false;
            }
        }else if (nFile==null){
            r = false;
        }
        nFile.delete();
        if (r) {
            responseData.put("url", qiniu_url+filePath);
            responseData.put(ResponseCode.MSG, "文件上传成功！");
        }else{
            responseData.put(ResponseCode.MSG, "文件上传失败！");
        }
        responseData.put(ResponseCode.CODE, r?1:0);
        renderJson(responseData);
    }

    public void playMusic(){
        int music_id = getParaToInt("music_id");
        setAttr("audio", Music.dao.findById(music_id));
        render("audioPlayer.html");
    }

}

package com.sagacity.docs.web.controller;

import com.jfinal.aop.Before;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.*;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.upload.UploadFile;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.system.*;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.service.Qiniu;
import com.sagacity.utility.ConvertUtil;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;

import java.io.File;
import java.util.List;

@ControllerBind(controllerKey = "/admin/system", viewPath = "/admin/system")
public class SystemController extends WebBaseController{

//    @Clear(WebLoginInterceptor.class)
//    @Before(Tx.class)
//    public void mergerUser(){
//        boolean r = false;
//        String sql = "select  wxa.*\n" +
//                "from wxapp_user wxa ";
//
//        for (Record wxa : Db.find(sql)){
//            String uid = UUID.randomUUID().toString();
//            r = new UserInfo().set("user_id", uid).set("_OpenID",wxa.getStr("open_id")).set("role_id", RoleType.USER)
//                    .set("account", PinyinUtil.getFirstLettersLo(wxa.getStr("nick_name")))
//                    .set("password", StringTool.generateMixString(6)).set("caption", wxa.getStr("nick_name"))
//                    .set("level", wxa.getInt("level_id")).set("created_at", wxa.get("created_at"))
//                    .set("creator_id", ConstantValue.DEFAULT_ADMINID).set("state", 1).save();
//            //附表
//            r = new UserProfile().set("user_id", uid).set("mobile_phone", wxa.get("mobile_phone"))
//                    .set("gender", wxa.get("gender")).set("nick_name", wxa.get("nick_name"))
//                    .set("avatar_url", wxa.get("avatar_url")).save();
//        }
//        renderJson(ResponseCode.RESULT, r);
//    }

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
        data.put(ResponseCode.LIST, ms);
        rest.success().setData(data);
        renderJson(rest);
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
        int classId = getParaToInt("id");
        renderJson(ResponseCode.DATA, DocClass.dao.findById(classId));
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
            rest.success("目录操作成功！");
        }else{
            rest.error("目录操作失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void delClass(){
        boolean r = false;
        int id = getParaToInt("id");
        if(Db.find("select * from doc_info where doc_class_id=?",id).size()>0){
            rest.error("目录包含文档数据，不允许删除！");
        }else if(Db.find("select * from doc_class where parent_id=?",id).size()>0){
            rest.error("目录包含下级目录，不允许删除！");
        }else {
            r = DocClass.dao.deleteById(id);
            if(r){
                rest.success("目录删除成功！");
            }else{
                rest.error("目录删除失败！");
            }
        }
        renderJson(rest);
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
        data.put(ResponseCode.LIST, Db.find(sql));
        rest.success().setData(data);
        renderJson(rest);
    }

    @Before(Tx.class)
    public void editMain(){

        Record data = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("data")))
                .set("updated_at", DateUtils.nowDateTime()).remove("main_type").remove("create_date");
        boolean r = Db.update("main", data);
        if(r){
            rest.success("更新成功！");
        }else{
            rest.error("更新失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void setMainState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update main set state=? where id=?", state, getPara("mainId"))>0? true:false;
        if(r){
            rest.success("设置成功！");
        }else{
            rest.error("设置失败！");
        }
        renderJson(rest);
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
                r = MainInfo.dao.findById(getPara("mainId")).set("cover", qiniu_url+filePath).update();
            }else{
                r = false;
            }
        }else if (nFile==null){
            r = false;
        }
        nFile.delete();
        if (r) {
            data.put("url", qiniu_url+filePath);
            rest.success("cover设置成功！").setData(data);
        }else{
            rest.error("cover设置失败！");
        }
        renderJson(rest);
    }


    /**
     * 视频分类标签
     */
    public void getTagList(){
        String sql = "select tag.id,tag.title,tag.desc,tag.css,tag.is_hot,tag.state from video_class tag\n" +
                "where 1=1\n" +
                "order by tag.created_at DESC";
        data.put(ResponseCode.LIST, Db.find(sql));
        rest.success().setData(data);
        renderJson(rest);
    }

    @Before(Tx.class)
    public void setTagState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        int tagId = getParaToInt("tagId");
        r = Db.update("update video_class set state=? where id=?", state, tagId)>0? true:false;
        if(r){
            rest.success("设置成功！");
        }else{
            rest.error("设置失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void setTagHot(){
        boolean r = false;
        int is_hot = getParaToBoolean("is_hot")? 1:0;
        int tagId = getParaToInt("tagId");
        r = Db.update("update video_class set is_hot=? where id=?", is_hot, tagId)>0? true:false;
        if(r){
            rest.success("设置成功！");
        }else{
            rest.error("设置失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void editTag(){

        Record data = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("data")));
        if (data.get("css").equals("null")){ //样式字段可选
            data.remove("css");
        }
        boolean r = Db.update("video_class", data);
        if(r){
            rest.success("更新成功！");
        }else{
            rest.error("更新失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void addTag(){
        boolean r = false;

        r = new VideoClass().set("title", getPara("name"))
                .set("order", 1).set("created_at", DateUtils.nowDateTime())
                .set("is_hot", 0).set("state", 1).save();
        if(r){
            rest.success("新增成功！");
        }else{
            rest.error("新增失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void delTag(){
        boolean r = false;
        int tagId = getParaToInt("tagId");
        if(Db.find("select * from video_info where video_class_id=?",tagId).size()>0){
            rest.error("分类包含视频数据，不允许删除！");
        }else {
            r = VideoClass.dao.deleteById(tagId);
            if(r){
                rest.success("分类删除成功！");
            }else{
                rest.error("分类删除失败！");
            }
        }
        renderJson(rest);
    }

    /**
     * 鸡汤库
     */
    public void getSoulList(){
        String sqlSelect = "select s.id,s.title,s.hits,s.state,s.created_at ";
        String sqlFrom = "from soul s\n";
        if(StringTool.notNull(getPara("key")) && StringTool.notBlank(getPara("key"))){
            sqlFrom += " where s.title like '%"+getPara("key")+"%'";
        }
        sqlFrom += " order by s.created_at DESC";

        Page<Record> dataList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
        rest.success().setData(dataList);
        renderJson(rest);
    }

    @Before(Tx.class)
    public void setSoulState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update soul set state=? where id=?", state, getPara("soulId"))>0? true:false;
        if(r){
            rest.success("设置成功！");
        }else{
            rest.error("设置失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void addSoul(){
        boolean r = false;

        Soul s = new Soul();
        r = s.set("title", getPara("title"))
                .set("state",1).set("created_at", DateUtils.nowDateTime()).set("hits",0).save();
        if(r){
            rest.success("章节增加成功！");
        }else{
            rest.error("章节增加失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void delSoul(){
        boolean r = false;

        Soul s =  Soul.dao.findById(getParaToInt("soulId"));
        r = s.delete();
        if(r){
            rest.success("删除成功！");
        }else{
            rest.error("删除失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void editSoul(){
        int soulId = getParaToInt("soulId");
        Soul s = Soul.dao.findById(soulId).set("title", getPara("title"));
        boolean r = s.update();
        if(r){
            rest.success("修改成功！");
        }else{
            rest.error("修改失败！");
        }
        renderJson(rest);
    }


    /**
     * 音乐库
     */

    public void getMusicList(){
        String sql_select = "select m.id,m.title,m.cover_url,m.resource_url,m.state,m.created_at,up.nick_name\n";
        String sql_from = "from music m\n" +
                "left join sys_users u on u.user_id=m.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where 1=1";

        if (StringTool.notNull(getPara("state")) && !StringTool.isBlank(getPara("state"))){
            sql_from += " and m.state="+ getPara("state");
        }
        sql_from += " order by created_at DESC";
        if (StringTool.notNull(getPara("pageIndex")) && !StringTool.isBlank(getPara("pageIndex"))){
            Page<Record> dataList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sql_select, sql_from);
            rest.success().setData(dataList);
        }else {
            data.put(ResponseCode.LIST, Db.find(sql_select + "\n" + sql_from));
            rest.success().setData(data);
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void setMusicState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = Db.update("update music set state=? where id=?", state, getPara("musicId"))>0? true:false;
        if(r){
            rest.success("设置成功！");
        }else{
            rest.error("设置失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void editMusic(){

        Record data = new Record().setColumns(ConvertUtil.jsonStrToMap(getPara("data")));
        data.remove("nick_name");
        boolean r = Db.update("music", data);
        if(r){
            rest.success("更新成功！");
        }else{
            rest.error("更新失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void addMusic(){
        boolean r = false;
        UserDao userInfo = getCurrentUser();

        r = new Music().set("title", getPara("name")).set("cover_url", "/assets/images/music_cover.png")
                .set("created_at", DateUtils.nowDateTime()).set("state", 0)
                .set("user_id", userInfo.getUser_id()).save();
        if(r){
            rest.success("新增成功！");
        }else{
            rest.error("新增失败！");
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void delMusic(){
        boolean r = false;
        UserDao userInfo = getCurrentUser();

        Music m =  Music.dao.findById(getParaToInt("musicId"));
        if(!m.getStr("user_id").equals(userInfo.getUser_id())){
            rest.error("非创建人不能删除！");
        }else{
            r = m.delete();
            if(r){
                rest.success("删除成功！");
            }else{
                rest.error("删除失败！");
            }
        }
        renderJson(rest);
    }

    @Before(Tx.class)
    public void uploadMusicFile(){
        UserDao userInfo = getCurrentUser();

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
                        .set("user_id", userInfo.getUser_id()).set("resource_url", qiniu_url+filePath).save();
            }else{
                r = false;
            }
        }else if (nFile==null){
            r = false;
        }
        nFile.delete();
        if (r) {
            data.put("url", qiniu_url+filePath);
            rest.success("文件上传成功！").setData(data);
        }else{
            rest.error("文件上传失败！");
        }
        renderJson(rest);
    }

    public void playMusic(){
        int musicId = getParaToInt("musicId");
        setAttr("audio", Music.dao.findById(musicId));
        render("audioPlayer.html");
    }

}

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
import com.sagacity.docs.model.doc.DocInfo;
import com.sagacity.docs.model.doc.DocPage;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.base.extend.RoleType;
import com.sagacity.docs.service.Qiniu;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;
import net.sf.json.JSONObject;

import java.io.File;
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

@ControllerBind(controllerKey = "/admin/document", viewPath = "/admin/document")
public class DocController extends WebBaseController {

    @Override
    public void index(){

    }

	public void getDocList(){
        JSONObject userInfo = getCurrentUser();

        String sqlSelect = SqlKit.sql("doc.getDocList-select");
        String sqlFrom = SqlKit.sql("doc.getDocList-from");
        if(userInfo.getInt("RoleID") == RoleType.ADMIN){
            sqlFrom += " where 1=1";
        }else{
            sqlFrom += " where doc.user_id='"+userInfo.get("UserID")+"'";
        }
        if(StringTool.notNull(getPara("key")) && StringTool.notBlank(getPara("key"))){
            sqlFrom += " and doc.title like '%"+getPara("key")+"%'";
        }
        sqlFrom += " order by doc.created_at Desc";
        if (StringTool.notNull(getPara("pageIndex")) && !StringTool.isBlank(getPara("pageIndex"))){
            Page<Record> noticeList = Db.paginate(getParaToInt("pageIndex", 1),
                    getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
            renderJson(convertPageData(noticeList));
        }else {
            renderJson(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom));
        }
    }

    public void editDoc(){
        int doc_id = getParaToInt("doc_id");
        String sql = SqlKit.sql("doc.getDocInfo");
        setAttr("doc", Db.findFirst(sql, doc_id));
        render("editDoc.html");
    }

    @Before(Tx.class)
    public void saveDoc(){
        boolean r = getModel(DocInfo.class, "doc")
                .set("updated_at", DateUtils.nowDateTime()).update();
        if(r){
            responseData.put(ResponseCode.MSG, "文档更新成功！");
        }else{
            responseData.put(ResponseCode.MSG, "文档更新失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void addDoc(){
        boolean r = false;
        JSONObject userInfo = getCurrentUser();
        DocInfo doc = getModel(DocInfo.class, "doc");
        r = doc.set("user_id", userInfo.get("UserID")).set("state",1).set("cover","/assets/images/doc_cover.png")
                .set("is_end",0).set("is_ad", 0).set("is_hot",0).set("created_at", DateUtils.nowDateTime()).save();
        //排序字段
        r = doc.set("order", doc.get("id")).update();
        if(r){
            responseData.put(ResponseCode.MSG, "文档新增成功！");
        }else{
            responseData.put(ResponseCode.MSG, "文档新增失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void delDoc(){
        boolean r = false;
        int doc_id= getParaToInt("doc_id");
        if(Db.findFirst("select * from doc_page where doc_id=?", doc_id) != null){
            responseData.put(ResponseCode.MSG, "文档已有章节数据，不允许删除！");
        }else{
            r = DocInfo.dao.deleteById(doc_id);
            if(r){
                responseData.put(ResponseCode.MSG, "文档删除成功！");
            }else{
                responseData.put(ResponseCode.MSG, "文档删除失败！");
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
            filePath = Qiniu.dao.uploadFile(nFile, upToken, "docs/cover/");
            if(filePath != ""){
                r = DocInfo.dao.findById(getPara("doc_id")).set("cover", qiniu_url+filePath).update();
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

    public void getClassTree(){
        int treeLevel = 1;

        String sql = "select dc.id,dc.title name,true open\n" +
                "from doc_class dc\n" +
                "where dc.parent_id=?\n" +
                "order by dc.order DESC";
        List<Record> ms = Db.find(sql, 0);
        addSubClassTree(ms ,treeLevel);
        renderJson(ms);
    }

    private void addSubClassTree(List<Record> ms, int tl){
        tl ++;
        String sql = "select dc.id,dc.title name,true open\n" +
                "from doc_class dc\n" +
                "where dc.parent_id=?\n" +
                "order by dc.order DESC";
        for(Record m : ms){ //循环找到下级
            List<Record> subs = Db.find(sql, m.getInt("id"));
            m.set("children", subs);
            if(tl > 2){ //控制展开的层级
                m.set("open", false);
            }
            if(subs.size()>0){
                addSubClassTree(subs, tl);
            }else{
                m.remove("children");
            }
        }
    }

    /**
     * 文档详情
     */
    public void docDetail(){
        int doc_id = getParaToInt("doc_id");
        DocInfo doc = DocInfo.dao.findById(doc_id);
        setAttr("doc", doc);
        render("docDetail.html");
    }

    public void getPageTree(){
        int docID = getParaToInt("doc_id");
        String sql = "select dp.id,dp.menu_title title,dp.order,dp.level,dp.parent_id\n" +
                ",case when level<=1 then true else false end spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";
        List<Record> ms = Db.find(sql, docID, 0);
        addSubPageTree(ms, docID);
        renderJson(ResponseCode.DATA, ms);
    }

    private void addSubPageTree(List<Record> ms, int docID){
        String sql = "select dp.id,dp.menu_title title,dp.order,dp.level,dp.parent_id\n" +
                ",case when level<=1 then true else false end spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";

        for(Record m : ms){ //循环找到下级
            List<Record> subs = Db.find(sql, docID, m.get("id"));
            m.set("children", subs);
            if(subs.size()>0){
                addSubPageTree(subs, docID);
            }
        }
    }

    @Before(Tx.class)
    public void addPage(){
        boolean r = false;
        String content = "## "+getPara("title");
        DocPage dp = new DocPage();
        r = dp.set("parent_id", getPara("pid")).set("title", getPara("title")).set("menu_title", getPara("title"))
                .set("content", content).set("level", getPara("level")).set("doc_id", getPara("doc_id"))
                .set("state",1).set("created_at", DateUtils.nowDateTime()).set("view_count",0).save();
        r = dp.set("order", dp.get("id")).update();
        if(r){
            responseData.put(ResponseCode.MSG, "章节增加成功！");
        }else{
            responseData.put(ResponseCode.MSG, "章节增加失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void delPage(){
        boolean r = false;
        int page_id = getParaToInt("page_id");
        if(Db.findFirst("select * from doc_page where parent_id=?", page_id) != null){
            responseData.put(ResponseCode.MSG, "包含下级章节，不允许删除！");
        }else{
            r = DocPage.dao.deleteById(page_id);
            if(r){
                responseData.put(ResponseCode.MSG, "章节删除成功！");
            }else{
                responseData.put(ResponseCode.MSG, "章节删除失败！");
            }
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    public void editPage(){
        int pageID = getParaToInt("page_id");
        setAttr("page", DocPage.dao.findById(pageID));
        render("editPage.html");
    }

    /**
     * 调整文档顺序 - 在同一个level中向上或向下调整
     */
    @Before({Tx.class})
    public void pageOrder(){
        boolean r = false;
        int pageID = getParaToInt("page_id");
        String adjustType = getPara("adjustType");

        DocPage sourcePage, targetPage;
        int sourceOrder, targetOrder;
        sourcePage = DocPage.dao.findById(pageID);
        sourceOrder = sourcePage.getInt("order");

        if(adjustType.equals("up")){
            String sqlUp = "select dp.* \n" +
                    "from doc_page dp\n" +
                    "left join doc_page dp1 on dp1.parent_id=dp.parent_id and dp1.doc_id=dp.doc_id\n" +
                    "where dp1.id=? and dp1.order > dp.order order by dp.order DESC limit 1";
            targetPage = DocPage.dao.findFirst(sqlUp, pageID);
            if(targetPage != null){
                targetOrder = targetPage.getInt("order");
                r = targetPage.set("order", sourceOrder).update();
                r = sourcePage.set("order", targetOrder).update();
            }
        }else if(adjustType.equals("down")){
            String sqlDown = "select dp.* \n" +
                    "from doc_page dp\n" +
                    "left join doc_page dp1 on dp1.parent_id=dp.parent_id and dp1.doc_id=dp.doc_id\n" +
                    "where dp1.id=? and dp1.order < dp.order order by dp.order ASC limit 1";
            targetPage = DocPage.dao.findFirst(sqlDown, pageID);
            if(targetPage != null){
                targetOrder = targetPage.getInt("order");
                r = targetPage.set("order", sourceOrder).update();
                r = sourcePage.set("order", targetOrder).update();
            }
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void modifyPage(){
        int pageID = getParaToInt("page_id");
        boolean r = DocPage.dao.findById(pageID).set("title", getPara("name"))
                .set("menu_title", getPara("name")).set("updated_at", DateUtils.nowDateTime()).update();
        if(r){
            responseData.put(ResponseCode.MSG, "章节修改成功！");
        }else{
            responseData.put(ResponseCode.MSG, "章节修改失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void saveContent(){
        DocPage dp = DocPage.dao.findById(getParaToInt("page_id"));
        boolean r = dp.set("content", getPara("content")).update();
        //修改文档本身的修改时间
        r = DocInfo.dao.findFirst("select * from doc_info where id=?", dp.getInt("doc_id"))
                .set("updated_at", DateUtils.nowDateTime()).update();
        if(r){
            responseData.put(ResponseCode.MSG, "章节保存成功！");
        }else{
            responseData.put(ResponseCode.MSG, "章节保存失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void uploadImg(){
        String filePath = "";
        String upToken = Qiniu.dao.getUploadToken(); //7牛上传token
        String qiniu_url = PropKit.get("qiniu.url");
        String config_dir = PropKit.get("resource.dir");
        boolean r= true;

        File f1 = new File(config_dir+"/imgTemp/");
        if (!f1.exists()) {
            f1.mkdirs();
        }
        UploadFile uploadFile = getFile("editormd-image-file", f1.getAbsolutePath());
        File nFile = uploadFile.getFile();
        if (nFile!=null) {
            //向7牛上传
            filePath = Qiniu.dao.uploadFile(nFile, upToken, "docs/"+getPara("doc_id")+"/");
            if(filePath != ""){

            }else{
                r = false;
            }
        }else if (nFile==null){
            r = false;
        }
        nFile.delete();
        if (r) {
            responseData.put("url", qiniu_url+filePath);
            responseData.put("message", "上传成功！");
        }else{
            responseData.put("message", "上传失败！");
        }
        responseData.put("success", r?1:0);
        renderJson(responseData);
    }

//    @Before(Tx.class)
//    public void setState(){
//        boolean r = false;
//        int state = getParaToBoolean("state")? 1:0;
//        r = DocInfo.dao.findById(getPara("doc_id")).set("state", state).update();
//        if(r){
//            responseData.put(ResponseCode.MSG, "设置成功！");
//        }else{
//            responseData.put(ResponseCode.MSG, "设置失败！");
//        }
//        responseData.put(ResponseCode.CODE, r? 1:0);
//        renderJson(responseData);
//    }

    @Before(Tx.class)
    public void setProperty(){
        boolean r = false;
        int property = getParaToBoolean("property")? 1:0;
        String type = getPara("type");
        switch (type){
            case "state":
                r = DocInfo.dao.findById(getPara("doc_id")).set("state", property).update();
                break;
            case "end" :
                r = DocInfo.dao.findById(getPara("doc_id")).set("is_end", property).update();
                break;
            case "hot" :
                r = DocInfo.dao.findById(getPara("doc_id")).set("is_hot", property).update();
                break;
            case "ad" :
                r = DocInfo.dao.findById(getPara("doc_id")).set("is_ad", property).update();
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
//    public void setEnd(){
//        boolean r = false;
//        int is_end = getParaToBoolean("is_end")? 1:0;
//        r = DocInfo.dao.findById(getPara("doc_id")).set("is_end", is_end).update();
//        if(r){
//            responseData.put(ResponseCode.MSG, "操作成功！");
//        }else{
//            responseData.put(ResponseCode.MSG, "操作失败！");
//        }
//        responseData.put(ResponseCode.CODE, r? 1:0);
//        renderJson(responseData);
//    }
//
//    @Before(Tx.class)
//    public void setHot(){
//        boolean r = false;
//        int is_hot = getParaToBoolean("is_hot")? 1:0;
//        r = DocInfo.dao.findById(getPara("doc_id")).set("is_hot", is_hot).update();
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
//        r = DocInfo.dao.findById(getPara("doc_id")).set("is_ad", is_ad).update();
//        if(r){
//            responseData.put(ResponseCode.MSG, "设置成功！");
//        }else{
//            responseData.put(ResponseCode.MSG, "设置失败！");
//        }
//        responseData.put(ResponseCode.CODE, r? 1:0);
//        renderJson(responseData);
//    }

    /**
     * 背景音乐选择
     */
    public void musicSelect(){
        int page_id = getParaToInt("page_id");
        setAttr("page", DocPage.dao.findById(page_id));
        render("musicSelect.html");
    }

    @Before(Tx.class)
    public void setMusic(){
        boolean r = DocPage.dao.findById(getPara("page_id")).set("music_id", getPara("music_id")).update();
        if(r){
            responseData.put(ResponseCode.MSG, "背景音乐设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "背景音乐设置失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

}

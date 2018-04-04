package com.sagacity.docs.doc;


import com.jfinal.aop.Before;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.json.JFinalJson;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.render.JsonRender;
import com.jfinal.upload.UploadFile;
import com.sagacity.docs.common.LoginValidator;
import com.sagacity.docs.common.WebBaseController;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.extend.RoleType;
import com.sagacity.docs.openapi.Qiniu;
import com.sagacity.docs.utility.DateUtils;
import com.sagacity.docs.utility.PropertiesFactoryHelper;
import com.sagacity.docs.utility.StringTool;
import freemarker.template.utility.DateUtil;
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

@ControllerBind(controllerKey = "/docs")
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
        JSONObject userInfo = getCurrentUser();
        DocInfo doc = getModel(DocInfo.class, "doc");
        boolean r = doc.set("user_id", userInfo.get("UserID")).set("state",1).set("cover","/assets/images/doc_cover.png")
                .set("is_end",0).set("order",1).set("is_hot",0).set("created_at", DateUtils.nowDateTime()).save();
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
        String qiniu_url = PropertiesFactoryHelper.getInstance().getConfig("qiniu.url");
        String config_dir = PropertiesFactoryHelper.getInstance().getConfig("resource.dir");
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
        String sql = "select dp.id,dp.menu_title name,dp.order,dp.parent_id,true spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order DESC";
        List<Record> ms = Db.find(sql, docID, 0);
        addSubMenu(ms, docID);
        renderJson(ResponseCode.DATA, ms);
    }

    private void addSubMenu(List<Record> ms, int docID){
        String sql = "select dp.id,dp.menu_title name,dp.order,dp.parent_id,true spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order DESC";

        for(Record m : ms){ //循环找到下级
            List<Record> subs = Db.find(sql, docID, m.get("id"));
            m.set("children", subs);
            if(subs.size()>0){
                addSubMenu(subs, docID);
            }
        }
    }

    @Before(Tx.class)
    public void addPage(){
        String content = "## "+getPara("title");
        boolean r = new DocPage()
                .set("parent_id", getPara("pid")).set("title", getPara("title")).set("menu_title", getPara("title"))
                .set("content", content).set("order", 1).set("doc_id", getPara("doc_id"))
                .set("state",1).set("created_at", DateUtils.nowDateTime()).set("view_count",0).save();
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
        boolean r = DocPage.dao.deleteById(getPara("page_id"));
        if(r){
            responseData.put(ResponseCode.MSG, "章节删除成功！");
        }else{
            responseData.put(ResponseCode.MSG, "章节删除失败！");
        }
        responseData.put(ResponseCode.CODE,r? 1:0);
        renderJson(responseData);
    }

    public void editPage(){
        int pageID = getParaToInt("page_id");
        setAttr("page", DocPage.dao.findById(pageID));
        render("editPage.html");
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
        int pageID = getParaToInt("page_id");
        boolean r = DocPage.dao.findById(pageID).set("content", getPara("content")).update();
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
        String qiniu_url = PropertiesFactoryHelper.getInstance().getConfig("qiniu.url");
        String config_dir = PropertiesFactoryHelper.getInstance().getConfig("resource.dir");
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

    @Before(Tx.class)
    public void setState(){
        boolean r = false;
        int state = getParaToBoolean("state")? 1:0;
        r = DocInfo.dao.findById(getPara("doc_id")).set("state", state).update();
        if(r){
            responseData.put(ResponseCode.MSG, "设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "设置失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void setHot(){
        boolean r = false;
        int is_hot = getParaToBoolean("is_hot")? 1:0;
        r = DocInfo.dao.findById(getPara("doc_id")).set("is_hot", is_hot).update();
        if(r){
            responseData.put(ResponseCode.MSG, "设置成功！");
        }else{
            responseData.put(ResponseCode.MSG, "设置失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

}

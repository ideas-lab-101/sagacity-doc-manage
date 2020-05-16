package com.sagacity.docs.web.controller;


import com.jfinal.aop.Clear;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.doc.DocInfo;
import com.sagacity.docs.model.doc.DocPage;
import com.sagacity.docs.service.SearchEngine;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.web.common.WebLoginInterceptor;
import com.sagacity.utility.StringTool;

import java.util.List;

@ControllerBind(controllerKey = "/", viewPath = "/")
public class MainController extends WebBaseController {

    @Override
    @Clear(WebLoginInterceptor.class)
    public void index(){
        render("index.html");
    }

    @Clear(WebLoginInterceptor.class)
    public void s(){
        String kw = getPara("kw");
        if(StringTool.notNull(kw)){
            render("search.html");
        }else{
            redirect("/");
        }
    }

    @Clear(WebLoginInterceptor.class)
    public void search(){
        boolean r = true;
        String kw = getPara("kw");

        String sqlSelect = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end,di.updated_at\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.UserID,u.Caption";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join sys_users u on u.UserID=di.user_id\n" +
                "where di.state=1 and di.title like '%"+kw+"%' or u.Caption like '%"+kw+"%'";;
        sqlFrom += "\n order by di.updated_at DESC";

        Page<Record> resultList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
        renderJson(convertPageData(resultList));
        renderJson(responseData);
    }

    /**
     * 文档主页
     */
    @Clear(WebLoginInterceptor.class)
    public void b(){
        int docID = getParaToInt("doc_id", 0);

        DocInfo dc = DocInfo.dao.findById(docID);

        if(dc != null){
            setAttr("doc", dc);
        }else{

        }
        render("main/bookMain.html");
    }

    /**
     * 文档详情页
     */
    @Clear(WebLoginInterceptor.class)
    public void d(){
        int docID = getParaToInt("doc_id", 0);
        int pageID = getParaToInt("page_id", 0);
        DocInfo dc = DocInfo.dao.findById(docID);

        if(dc != null){
            setAttr("doc", dc);
            setAttr("pageID", pageID);
        }else{

        }

        render("main/docMain.html");
    }

    @Clear(WebLoginInterceptor.class)
    public void getPageTree(){
        int docID = getParaToInt("doc_id");
        String sql = "select dp.id,dp.menu_title title,dp.order,dp.level,dp.parent_id\n" +
                ",case when level<=2 then true else false end spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";
        List<Record> ms = Db.find(sql, docID, 0);
        addSubPageTree(ms, docID);
        renderJson(ResponseCode.DATA, ms);
    }

    private void addSubPageTree(List<Record> ms, int docID){
        String sql = "select dp.id,dp.menu_title title,dp.order,dp.level,dp.parent_id\n" +
                ",case when level<=2 then true else false end spread\n" +
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

    @Clear(WebLoginInterceptor.class)
    public void p(){
        int pageID = getParaToInt("page_id");

        DocPage dp = DocPage.dao.findById(pageID);

        if(dp != null){
            setAttr("page", dp);
        }else{

        }
        render("main/pageMain.html");
    }

    @Clear(WebLoginInterceptor.class)
    public void qr(){
        int docID = getParaToInt("doc_id");
        render("main/qrCode.html");
    }

    /**
     * 视频主页
     */
    @Clear(WebLoginInterceptor.class)
    public void v(){

    }
}

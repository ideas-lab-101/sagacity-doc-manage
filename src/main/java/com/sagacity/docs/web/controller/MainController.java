package com.sagacity.docs.web.controller;

import com.alibaba.fastjson.JSONArray;
import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.JsonKit;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.doc.DocInfo;
import com.sagacity.docs.model.doc.DocPage;
import com.sagacity.docs.model.system.DocClass;
import com.sagacity.docs.service.MindGenerator;
import com.sagacity.docs.service.SearchEngine;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.web.common.WebLoginInterceptor;
import com.sagacity.utility.ConvertUtil;
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
            render("main/search.html");
        }else{
            redirect("/");
        }
    }

    @Clear(WebLoginInterceptor.class)
    public void seg(){
        String kw = getPara("kw");
        String result = SearchEngine.dao.seg(kw);
        responseData.put(ResponseCode.DATA, result);
        renderJson(responseData);
    }

    @Clear(WebLoginInterceptor.class)
    public void search(){
        boolean r = true;
        String kw = getPara("kw");

        String sqlSelect = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end,SUBSTR(di.updated_at,1,10) update_date\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.UserID,u.Caption,dp.view_count,dp.page_count";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join (select count(id) page_count,sum(view_count) view_count, doc_id from doc_page group by doc_id) dp on dp.doc_id=di.id\n" +
                "left join sys_users u on u.UserID=di.user_id\n" +
                "where di.state=1 and di.title like '%"+kw+"%' or u.Caption like '%"+kw+"%'";
        sqlFrom += "\n order by di.updated_at DESC";

        Page<Record> resultList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
        renderJson(convertPageData(resultList));
        responseData.put("seg", SearchEngine.dao.seg(kw));
        renderJson(responseData);
    }

    /**
     * 文档主页
     */
    @Clear(WebLoginInterceptor.class)
    public void b(){
        int docId = getParaToInt("docId", 0);
        //文档信息
        Record dc = Db.findFirst(SqlKit.sql("doc.getDocInfo"), docId);
        Record au = Db.findFirst(SqlKit.sql("user.getAuthorInfo")+
                " where u.UserID=?", dc.getStr("user_id"));
        if(dc != null){
            setAttr("doc", dc);
            setAttr("au", au);
        }else{

        }
        render("main/bookMain.html");
    }

    /**
     * 作者首页
     */
    @Clear(WebLoginInterceptor.class)
    public void a(){
        String userId = getPara("userId");
        String sql = SqlKit.sql("user.getAuthorInfo") + " where u.UserID=?";
        Record ui = Db.findFirst(sql, userId);
        setAttr("author", ui);
        //基于作者的统计
        String sql1 = "select count(di.id) docCount from doc_info di\n" +
                "where di.user_id=?";
        Record st = new Record();
        st.set("docCount", Db.findFirst(sql1, userId).getInt("docCount"));
        setAttr("st", st);
        render("main/authorMain.html");
    }

    /**
     * 作者列表
     */
    @Clear(WebLoginInterceptor.class)
    public void authorBook(){
        String userId = getPara("userId");

        String sqlSelect = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end,SUBSTR(di.updated_at,1,10) update_date\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.UserID,u.Caption,dp.view_count,dp.page_count";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join (select count(id) page_count,sum(view_count) view_count, doc_id from doc_page group by doc_id) dp on dp.doc_id=di.id\n" +
                "left join sys_users u on u.UserID=di.user_id\n" +
                "where di.state=1 and di.user_id ='"+userId+"'";
        sqlFrom += "\n order by di.updated_at DESC";

        Page<Record> resultList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
        renderJson(convertPageData(resultList));
        renderJson(responseData);
    }

    /**
     * 分类首页
     */
    @Clear(WebLoginInterceptor.class)
    public void c(){
        int classId = getParaToInt("classId", 0);
        setAttr("classs", DocClass.dao.findById(classId));
        String sql = "select dcc.* \n" +
                "from doc_class dc\n" +
                "left join doc_class dcc on dcc.parent_id=dc.parent_id\n" +
                "where dc.id=?";
        setAttr("relatedList", Db.find(sql, classId));
        render("main/classMain.html");
    }

    /**
     * 分类列表
     */
    @Clear(WebLoginInterceptor.class)
    public void classBook(){
        String classId = getPara("classId");

        responseData.put("classs", DocClass.dao.findById(classId));
        String sqlSelect = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end,SUBSTR(di.updated_at,1,10) update_date\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.UserID,u.Caption,dp.view_count,dp.page_count";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join (select count(id) page_count,sum(view_count) view_count, doc_id from doc_page group by doc_id) dp on dp.doc_id=di.id\n" +
                "left join sys_users u on u.UserID=di.user_id\n" +
                "where di.state=1 and di.doc_class_id ='"+classId+"'";
        sqlFrom += "\n order by di.updated_at DESC";

        Page<Record> resultList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 12), sqlSelect, sqlFrom);
        renderJson(convertPageData(resultList));
        renderJson(responseData);
    }

    /**
     * 文档详情页
     */
    @Clear(WebLoginInterceptor.class)
    public void d(){
        int docId = getParaToInt("docId", 0);
        int pageId = getParaToInt("pageId", 0);
        DocInfo dc = DocInfo.dao.findById(docId);

        if(dc != null){
            setAttr("doc", dc);
            setAttr("pageId", pageId);
        }else{

        }
        render("main/docMain.html");
    }

    /**
     * 生成思维图
     */
    @Clear(WebLoginInterceptor.class)
    public void exportBook(){
        boolean r = true;

        int docId = getParaToInt("docId");
        DocInfo doc = DocInfo.dao.findById(docId);
        String sql = "select dp.id,dp.menu_title title,dp.order,dp.level,dp.parent_id\n" +
                ",case when level<=2 then true else false end spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";
        List<Record> ms = Db.find(sql, docId, 0);
        addSubPageTree(ms, docId);
        String bookName = doc.getStr("title");
        r = MindGenerator.dao.generateMindMap(docId, bookName, JsonKit.toJson(ms));
        if(r){
            responseData.put(ResponseCode.CODE, 1);
            responseData.put(ResponseCode.DATA, PropKit.get("resource.url") + "mind_map/" + bookName + ".xmind");
        }else{
            responseData.put(ResponseCode.CODE, 0);
            responseData.put(ResponseCode.MSG, "操作失败！");
        }
        renderJson(responseData);
    }

    @Clear(WebLoginInterceptor.class)
    public void getPageTree(){
        int docId = getParaToInt("docId");
        String sql = "select dp.id,dp.menu_title title,dp.order,dp.level,dp.parent_id\n" +
                ",case when level<=2 then true else false end spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";
        List<Record> ms = Db.find(sql, docId, 0);
        addSubPageTree(ms, docId);
        renderJson(ResponseCode.DATA, ms);
    }

    private void addSubPageTree(List<Record> ms, int docId){
        String sql = "select dp.id,dp.menu_title title,dp.order,dp.level,dp.parent_id\n" +
                ",case when level<=2 then true else false end spread\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";

        for(Record m : ms){ //循环找到下级
            List<Record> subs = Db.find(sql, docId, m.get("id"));
            m.set("children", subs);
            if(subs.size()>0){
                addSubPageTree(subs, docId);
            }
        }
    }

    @Clear(WebLoginInterceptor.class)
    public void p(){
        int pageId = getParaToInt("pageId");
        DocPage dp = DocPage.dao.findById(pageId);

        if(dp != null){
            setAttr("page", dp);
        }else{

        }
        render("main/pageMain.html");
    }

    @Clear(WebLoginInterceptor.class)
    public void qr(){
        int docId = getParaToInt("docId");
        render("main/qrCode.html");
    }

    /**
     * 视频主页
     */
    @Clear(WebLoginInterceptor.class)
    public void v(){

    }
}

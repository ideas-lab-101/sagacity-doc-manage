package com.sagacity.docs.wxapp.controller.v2;

import com.jfinal.aop.Clear;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.sagacity.docs.model.UserDao;
import com.sagacity.docs.model.doc.DocPage;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.wxapp.common.WXSSBaseController;
import com.sagacity.docs.wxapp.common.WXSSLoginInterceptor;
import com.sagacity.utility.StringTool;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.builder.Extension;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.*;

/**
 * Created by mulaliu on 16/4/15.
 */
@ControllerBind(controllerKey = "/wxss/doc/v2", viewPath = "/wxss")
public class DocController extends WXSSBaseController {


    @Override
    @Clear(WXSSLoginInterceptor.class)
    public void index() {

        //swiper数据
        data.put("swiper", Db.find(SqlKit.sql("doc.getMainList"), 1));
        //首页宫格数据
        data.put("grid", Db.find(SqlKit.sql("doc.getMainList"),2));
        //分类热文
        List<Record> dcs = Db.find(SqlKit.sql("doc.getClassList"),1);
        for (Record dc : dcs){
            List<Record> ds = Db.find("select doc.id, doc.title, IFNULL(dc.view_count,0) view_count, doc.desc, doc.cover, doc.is_hot, doc.is_end, doc.doc_class_id\n" +
                    "from doc_info doc\n" +
                    "left join (select sum(view_count) view_count, doc_id from doc_page group by doc_id) dc on dc.doc_id=doc.id\n" +
                    "where doc.state=1 and doc.is_hot=1 and doc.doc_class_id=?", dc.getInt("id"));
            dc.set("doc", ds);
        }
        data.put("doc", dcs);
        rest.success().setData(data);
        renderJson(rest);
    }

    public void getClassList(){

        String sql = "select dc.*\n" +
                "from doc_class dc\n" +
                "where dc.state=1 and dc.parent_id = ?\n" +
                "order by dc.order DESC";
        List<Record> dc = new ArrayList<Record>();
        for (Record p : Db.find(sql, 0)){
            List<Record> d1 = Db.find(sql, p.get("id"));
            for (Record c : d1){
                c.set("son", Db.find(sql, c.get("id")));
            }
            dc.addAll(d1);
        }
        rest.success().setData(dc);
        renderJson(rest);
    }

    /**
     * 获得分类文档列表
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getClassDocList(){
        int page = getParaToInt("page"); //数据分页
        int classId = getParaToInt("classId", 0);

        String sql_select = "select di.id,di.cover,di.title,di.desc,is_end, ifNULL(dc.view_count,0) view_count";
        String sql_from = "from doc_info di\n" +
                "left join (select sum(view_count) view_count, doc_id from doc_page group by doc_id) dc on dc.doc_id=di.id\n" +
                "where di.state=1";
        //按作者过滤
        if(StringTool.notNull(getPara("userId")) && StringTool.notBlank(getPara("userId")) ){
            sql_from += " and di.user_id='"+getPara("userId")+"'";
        }
        if(classId == -1) {//特殊处理
            sql_from += " and DATEDIFF(now(),di.updated_at)<20 order by di.updated_at DESC";
        }else if(classId == 0){
            sql_from += " order by di.created_at DESC";
        }else{
            sql_from += " and di.doc_class_id=" + classId + " order by di.created_at DESC";
        }

        Page<Record> docList = Db.paginate(page,  getParaToInt("pageSize", 12),
                sql_select, sql_from);
        //添加图片附件
        rest.success().setData(docList);
        renderJson(rest);
    }

    /**
     * 获得关联的文档
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getRelatedDocList(){
        int page = getParaToInt("page"); //数据分页
        int docId = getParaToInt("docId");

        String sqlSelect = "select di.id,di.cover,di.title,di.desc,di.is_end\n";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_info dl on dl.doc_class_id=di.doc_class_id\n" +
                "where di.state=1 and dl.id=? and di.id != ?";

        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            Page<Record> docList = Db.paginate(page,
                    getParaToInt("pageSize", 10), sqlSelect, sqlFrom, docId, docId);
            rest.success().setData(docList);
        }else {
            data.put(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom, docId, docId));
            rest.success().setData(data);
        }
        renderJson(rest);
    }

    /**
     * 文档详情
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getDocInfo(){
        UserDao userInfo = getCurrentUser(getPara("token"));
        int docId = getParaToInt("docId", 0);
        //获得文档相关信息
        Record doc = Db.findFirst(SqlKit.sql("doc.getDocInfo"), docId);
        //是否已添加收藏
        data.put("is_favor", false);
        if(userInfo != null){
            if(Db.findFirst("select * from user_favor where user_id=? and data_id=? and type=?", userInfo.getUser_id(), docId, "doc") != null){
                data.put("is_favor", true);
            }
        }
        data.put("doc", doc);
        //点赞人员列表
        data.put("likes", Db.find("select u.user_id,up.nick_name,up.avatar_url " +
                "from user_like ul\n" +
                "left join sys_users u on u.user_id=ul.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where ul.data_id=? and ul.type=?\n" +
                "order by ul.created_at DESC", docId, "doc"));
        //相关文档
        String sqlRC = "select di.id,di.cover,di.title,di.desc,di.is_end, ifNULL(dc.view_count,0) view_count\n" +
                "from doc_info di\n" +
                "left join (select sum(view_count) view_count, doc_id from doc_page group by doc_id) dc on dc.doc_id=di.id\n" +
                "left join doc_info dl on dl.doc_class_id=di.doc_class_id\n" +
                "where di.state=1 and dl.id=? and di.id != ?\n" +
                "limit 6";
        data.put("relatedDocs", Db.find(sqlRC, docId, docId));
        rest.success().setData(data);
        renderJson(rest);

    }

    @Clear(WXSSLoginInterceptor.class)
    public void getDocMenu(){
        int docId= getParaToInt("docId");
        String sql = "select dp.id,dp.title,dp.menu_title,dp.order,dp.parent_id\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";

        List<Record> ms = Db.find(sql, docId, 0);
        addSubMenu(ms, docId);
        data.put(ResponseCode.LIST, ms);
        rest.success().setData(data);
        renderJson(rest);
    }

    /**
     * 添加children
     */
    private void addSubMenu(List<Record> ms, int docId){
        String sql = "select dp.id,dp.title,dp.menu_title,dp.order,dp.parent_id\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";

        for(Record m : ms){ //循环找到下级
            List<Record> subs = Db.find(sql, docId, m.get("id"));
            m.set("children", subs);
            if(subs.size()>0){
                addSubMenu(subs, docId);
            }
        }
    }

    @Clear(WXSSLoginInterceptor.class)
    public void getPageDetail(){
        int pageId = getParaToInt("pageId");

        DocPage page = DocPage.dao.findById(pageId);
        page.set("view_count", page.getInt("view_count")+1).update();
        data.put("page", page);
        data.put("likes", Db.find("select u.user_id,up.nick_name,up.avatar_url\n" +
                "from user_like ul\n" +
                "left join sys_users u on u.user_id=ul.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where ul.data_id=? and ul.type=?\n" +
                "order by ul.created_at DESC", pageId, "page"));
        //菜单
        String sql = "select dp.id,dp.doc_id,dp.title,dp.menu_title,dp.order,dp.parent_id\n" +
                "from doc_page dp\n" +
                "left join doc_page dp1 on dp1.id=dp.parent_id\n" +
                "where dp.doc_id=?\n" +
                "order by dp.level,ifnull(dp1.order,dp.order),dp.order";
        List<Record> ms = Db.find(sql, page.getInt("doc_id"));
        data.put("menu", ms);
        rest.success().setData(data);
        renderJson(rest);
    }

//    public void getHotSearch(){
//        String sql = "select name,count(name) searchCount\n" +
//                "from doc_search ds\n" +
//                "group by ds.name\n" +
//                "order by searchCount DESC";
//        data.put(ResponseCode.LIST, Db.find(sql));
//        rest.success().setData(data);
//        renderJson(rest);
//    }

    /**
     * 结果搜索
     */
//    public void search(){
//        String key = getPara("key");
//        String sql = "select dc.id,dc.cover,dc.title from doc_info dc\n" +
//                "where dc.title like '%"+key+"%'";
//        data.put("doc", Db.find(sql));
//        //全文检索结果
//        Map<String, Object> result = new HashMap<String, Object>();
//        result.put("total", 0);
//        result.put("searchTime", 0);
//        result.put("items", null);
//        data.put("result", result);
//        rest.success().setData(data);
//        renderJson(rest);
//    }

    /**
     * 提示搜索
     */
//    public void tipSearch(){
//
//    }

    /**
     * 获得MD转换得到的html内容
     */
    @Clear(WXSSLoginInterceptor.class)
    public void getPageHtml(){
        int pageId = getParaToInt("pageId");

        String md = DocPage.dao.findById(pageId).getStr("content");
        MutableDataSet options = new MutableDataSet();
        options.setFrom(ParserEmulationProfile.MARKDOWN);
        options.set(Parser.EXTENSIONS, Arrays.asList(new Extension[] { TablesExtension.create()}));

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(md);
        String html = renderer.render(document);
        rest.success().setData(html);

        renderJson(rest);
    }

    /**
     * 用于H5渲染
     */
    @Clear(WXSSLoginInterceptor.class)
    public void renderPageHtml(){
        int pageId = getParaToInt("pageId");

        DocPage dp = DocPage.dao.findById(pageId);
        String md = dp.getStr("content");

        MutableDataSet options = new MutableDataSet();
        options.setFrom(ParserEmulationProfile.MARKDOWN);
        options.set(Parser.EXTENSIONS, Arrays.asList(new Extension[] { TablesExtension.create()}));

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(md);
        String html = renderer.render(document);
        setAttr("title", dp.get("title"));
        setAttr("content", html);

        render("/template/article.html");
    }

    public void docFeedback(){
        boolean r = true;

        if(r){
            rest.success("谢谢反馈！");
        }else {
            rest.error("提交失败！");
        }
        renderJson(rest);
    }

}

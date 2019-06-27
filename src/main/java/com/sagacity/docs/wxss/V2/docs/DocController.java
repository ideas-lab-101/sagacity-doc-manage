package com.sagacity.docs.wxss.V2.docs;

import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.sagacity.docs.doc.DocPage;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.wxss.WXSSBaseController;
import com.sagacity.utility.StringTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mulaliu on 16/4/15.
 */
@ControllerBind(controllerKey = "/wxss/v2/doc", viewPath = "/wxss")
public class DocController extends WXSSBaseController {


    @Override
    public void index() {

        //swiper数据
        responseData.put("swiper", Db.find(SqlKit.sql("doc.getMainList"), 1));
        //首页宫格数据
        responseData.put("grid", Db.find(SqlKit.sql("doc.getMainList"),2));
        //分类热文
        List<Record> dcs = Db.find(SqlKit.sql("doc.getClassList"),1);
        for (Record dc : dcs){
            List<Record> ds = Db.find("select doc.id, doc.title, IFNULL(dc.view_count,0) view_count, doc.desc, doc.cover, doc.is_hot, doc.is_end, doc.doc_class_id\n" +
                    "from doc_info doc\n" +
                    "left join (select sum(view_count) view_count, doc_id from doc_page group by doc_id) dc on dc.doc_id=doc.id\n" +
                    "where doc.state=1 and doc.is_hot=1 and doc.doc_class_id=?", dc.get("id"));
            dc.set("doc", ds);
        }
        responseData.put("doc", dcs);
        renderJson(responseData);
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
        renderJson(dc);
    }

    /**
     * 获得分类文档列表
     */
    public void getClassDocList(){
        int page = getParaToInt("page"); //数据分页
        int classID = getParaToInt("class_id", 0);

        String sql_select = "select di.id,di.cover,di.title,di.desc,is_end, ifNULL(dc.view_count,0) view_count";
        String sql_from = "from doc_info di\n" +
                "left join (select sum(view_count) view_count, doc_id from doc_page group by doc_id) dc on dc.doc_id=di.id\n" +
                "where di.state=1";
        //按作者过滤
        if(StringTool.notNull(getPara("user_id")) && StringTool.notBlank(getPara("user_id")) ){
            sql_from += " and di.user_id='"+getPara("user_id")+"'";
        }
        if(classID == -1) {//特殊处理
            sql_from += " and DATEDIFF(now(),di.updated_at)<20 order by di.updated_at DESC";
        }else if(classID == 0){
            sql_from += " order by di.created_at DESC";
        }else{
            sql_from += " and di.doc_class_id=" + classID + " order by di.created_at DESC";
        }

        Page<Record> docList = Db.paginate(page,  getParaToInt("pageSize", 12),
                sql_select, sql_from);
        //添加图片附件
        renderJson(docList);
    }

    /**
     * 获得关联的文档
     */
    public void getRelatedDocList(){
        int page = getParaToInt("page"); //数据分页
        int docID = getParaToInt("doc_id");

        String sqlSelect = "select di.id,di.cover,di.title,di.desc,di.is_end\n";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_info dl on dl.doc_class_id=di.doc_class_id\n" +
                "where di.state=1 and dl.id=? and di.id != ?";

        if (StringTool.notNull(getPara("page")) && !StringTool.isBlank(getPara("page"))){
            renderJson(Db.paginate(page,
                    getParaToInt("pageSize", 10), sqlSelect, sqlFrom, docID, docID));
        }else {
            renderJson(ResponseCode.LIST, Db.find(sqlSelect + "\n" + sqlFrom, docID, docID));
        }
    }

    /**
     * 文档详情
     */
    public void getDocInfo(){
        int docID = getParaToInt("doc_id", 0);
        //获得文档相关信息
        Record doc = Db.findFirst(SqlKit.sql("doc.getDocInfo"), docID);
        //是否已添加收藏
        doc.set("is_favor", false);
        if(StringTool.notNull(getPara("token"))){
            if(Db.findFirst("select * from user_favor where open_id=? and data_id=? and type=?", getPara("token"), docID, "doc") != null){
                doc.set("is_favor", true);
            }
        }
        responseData.put("doc", doc);
        //点赞人员列表
        responseData.put("likes", Db.find("select user.open_id,user.avatar_url,user.nick_name " +
                "from user_like ul\n" +
                "left join wx_user user on user.open_id=ul.open_id\n" +
                "where ul.data_id=? and ul.type=?\n" +
                "order by ul.created_at DESC", docID, "doc"));
        //相关文档
        String sqlRC = "select di.id,di.cover,di.title,di.desc,di.is_end, ifNULL(dc.view_count,0) view_count\n" +
                "from doc_info di\n" +
                "left join (select sum(view_count) view_count, doc_id from doc_page group by doc_id) dc on dc.doc_id=di.id\n" +
                "left join doc_info dl on dl.doc_class_id=di.doc_class_id\n" +
                "where di.state=1 and dl.id=? and di.id != ?\n" +
                "limit 6";
        responseData.put("relatedDocs", Db.find(sqlRC, docID, docID));
        renderJson(responseData);

    }

    public void getDocMenu(){
        int docID = getParaToInt("doc_id");
        String sql = "select dp.id,dp.title,dp.menu_title,dp.order,dp.parent_id\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";

        List<Record> ms = Db.find(sql, docID, 0);
        addSubMenu(ms, docID);
        renderJson(ResponseCode.DATA, ms);
    }

    /**
     * 添加children
     */
    private void addSubMenu(List<Record> ms, int docID){
        String sql = "select dp.id,dp.title,dp.menu_title,dp.order,dp.parent_id\n" +
                "from doc_page dp\n" +
                "where dp.doc_id=? and dp.parent_id=?\n" +
                "order by dp.order";

        for(Record m : ms){ //循环找到下级
            List<Record> subs = Db.find(sql, docID, m.get("id"));
            m.set("children", subs);
            if(subs.size()>0){
                addSubMenu(subs, docID);
            }
        }
    }

    public void getPageDetail(){
        int pageID = getParaToInt("page_id");

        DocPage page = DocPage.dao.findById(pageID);
        page.set("view_count", page.getInt("view_count")+1).update();
        responseData.put("page", page);
        responseData.put("likes", Db.find("select user.open_id,user.avatar_url,user.nick_name " +
                "from user_like ul\n" +
                "left join wx_user user on user.open_id=ul.open_id\n" +
                "where ul.data_id=? and ul.type=?\n" +
                "order by ul.created_at DESC", pageID, "page"));
        //菜单
        String sql = "select dp.id,dp.doc_id,dp.title,dp.menu_title,dp.order,dp.parent_id\n" +
                "from doc_page dp\n" +
                "left join doc_page dp1 on dp1.id=dp.parent_id\n" +
                "where dp.doc_id=?\n" +
                "order by dp.level,ifnull(dp1.order,dp.order),dp.order";
        List<Record> ms = Db.find(sql, page.get("doc_id"));
        responseData.put("menu", ms);
        renderJson(responseData);
    }

    public void getHotSearch(){
        String sql = "select name,count(name) searchCount\n" +
                "from doc_search ds\n" +
                "group by ds.name\n" +
                "order by searchCount DESC";
        renderJson(ResponseCode.LIST, Db.find(sql));
    }

    /**
     * 结果搜索
     */
    public void search(){
        String key = getPara("key");
        String sql = "select dc.id,dc.cover,dc.title from doc_info dc\n" +
                "where dc.title like '%"+key+"%'";
        responseData.put("doc", Db.find(sql));
        //全文检索结果
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("total", 0);
        result.put("searchtime", 0);
        result.put("items", null);
        responseData.put("result", result);
        renderJson(responseData);
    }

    /**
     * 提示搜索
     */
    public void tipSearch(){

    }

    public void docFeedback(){
        boolean r = true;

        if(r){
            responseData.put(ResponseCode.MSG, "谢谢反馈！");
        }else {
            responseData.put(ResponseCode.MSG, "提交失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1: 0);
        renderJson(responseData);
    }

}

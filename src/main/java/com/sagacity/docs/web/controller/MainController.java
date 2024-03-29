package com.sagacity.docs.web.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Clear;
import com.jfinal.ext.plugin.sqlinxml.SqlKit;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.kit.HashKit;
import com.jfinal.kit.JsonKit;
import com.jfinal.kit.PathKit;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;
import com.sagacity.docs.base.extend.CacheKey;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.doc.DocInfo;
import com.sagacity.docs.model.doc.DocPage;
import com.sagacity.docs.model.system.DocClass;
import com.sagacity.docs.service.BullshitGenerator;
import com.sagacity.docs.service.MindGenerator;
import com.sagacity.docs.service.PageGenerator;
import com.sagacity.docs.service.SearchEngine;
import com.sagacity.docs.web.common.WebBaseController;
import com.sagacity.docs.web.common.WebLoginInterceptor;
import com.sagacity.utility.DateUtils;
import com.sagacity.utility.StringTool;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerBind(controllerKey = "/", viewPath = "/")
public class MainController extends WebBaseController {

    @Override
    @Clear(WebLoginInterceptor.class)
    public void index(){
        render("index.html");
    }

    @Clear(WebLoginInterceptor.class)
    public void test(){
        String result = "";
        //测试汉字转拼音
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        try{
            char[ ] strArr = "硬是要的".toCharArray();
            for(char c : strArr){
                String[] rs = PinyinHelper.toHanyuPinyinStringArray(c, format);
                for(String s : rs){
                    System.out.println(s);
                }
                System.out.println("===============");
            }
            result = PinyinHelper.toHanyuPinyinString("硬是要的", format, " ");
        }catch (BadHanyuPinyinOutputFormatCombination ex){
            ex.printStackTrace();
        }
        rest.success().setData(result);
        renderJson(rest);
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
        rest.success().setData(result);
        renderJson(rest);
    }

    @Clear(WebLoginInterceptor.class)
    public void search(){
        boolean r = true;
        String kw = getPara("kw");
        List<String> segList = SearchEngine.dao.segList(kw);
        StringBuilder segs = new StringBuilder();

        String sqlSelect = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end,SUBSTR(di.updated_at,1,10) update_date\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.user_id,up.nick_name,dp.view_count,ifNULL(dp.page_count,0) page_count";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join (select count(id) page_count,sum(view_count) view_count, doc_id from doc_page group by doc_id) dp on dp.doc_id=di.id\n" +
                "left join sys_users u on u.user_id=di.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where di.state=1 and (";

        for (String key : segList) {
            if (segs.length() != 0) {
                sqlFrom += " or ";
                segs.append(" ");
            }
            sqlFrom += "di.title like '%"+key+"%' or up.nick_name like '%"+key+"%'";
            segs.append(key);
        }
        sqlFrom += ") \n order by di.updated_at DESC";

        Page<Record> dataList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
        data.put(ResponseCode.PAGE, dataList);
        data.put("segs", segs.toString());
        rest.success().setData(data);
        //更好的写法
        renderJson(rest);
    }

    /**
     * 文档主页
     */
    @Clear(WebLoginInterceptor.class)
    public void b(){
        int docId = getParaToInt("docId", 0);
        //文档信息
        Record dc = Db.findFirst(SqlKit.sql("doc.getDocInfo"), docId);
        Record au = Db.findFirst(SqlKit.sql("user.getAccountInfo")+
                " where u.user_id=?", dc.getStr("user_id"));
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
        String sql = SqlKit.sql("user.getAccountInfo") + " where u.user_id=?";
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
                ",dc.id doc_class_id,dc.title doc_class,u.user_id,up.nick_name,dp.view_count,dp.page_count";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join (select count(id) page_count,sum(view_count) view_count, doc_id from doc_page group by doc_id) dp on dp.doc_id=di.id\n" +
                "left join sys_users u on u.user_id=di.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where di.state=1 and di.user_id ='"+userId+"'";
        sqlFrom += "\n order by di.updated_at DESC";

        Page<Record> dataList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 10), sqlSelect, sqlFrom);
        rest.success().setData(dataList);
        renderJson(rest);
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

        data.put("classs", DocClass.dao.findById(classId));
        String sqlSelect = "select di.id,di.title,di.`desc`,di.cover,di.source,di.is_end,SUBSTR(di.updated_at,1,10) update_date\n" +
                ",dc.id doc_class_id,dc.title doc_class,u.user_id,up.nick_name,dp.view_count,dp.page_count";
        String sqlFrom = "from doc_info di\n" +
                "left join doc_class dc on dc.id=di.doc_class_id\n" +
                "left join (select count(id) page_count,sum(view_count) view_count, doc_id from doc_page group by doc_id) dp on dp.doc_id=di.id\n" +
                "left join sys_users u on u.user_id=di.user_id\n" +
                "left join user_profile up on up.user_id=u.user_id\n" +
                "where di.state=1 and di.doc_class_id ='"+classId+"'";
        sqlFrom += "\n order by di.updated_at DESC";

        Page<Record> dataList = Db.paginate(getParaToInt("pageIndex", 1),
                getParaToInt("pageSize", 12), sqlSelect, sqlFrom);
        data.put(ResponseCode.PAGE, dataList);
        rest.success().setData(data);
        renderJson(rest);
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
            data.put("url", PropKit.get("resource.url") + "mind_map/" + bookName + ".xmind");
            rest.success().setData(data);
        }else{
            rest.error("操作失败！");
        }
        renderJson(rest);
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

    @Clear(WebLoginInterceptor.class)
    public void soul(){
        int id = getParaToInt("id", 0);
        setAttr("id", id);
        render("main/soul.html");
    }

    /**
     * 毒鸡汤
     */
    @Clear(WebLoginInterceptor.class)
    public void getSoul(){
        int id = getParaToInt("id", 0);
        String sql = "";
        if (id == 0){
            sql = "SELECT * from soul \n" +
                    "where state=1 \n" +
                    "ORDER BY RAND() LIMIT 1\n";
        }else{
            sql = "SELECT * from soul \n" +
                    "where id="+ id;
        }
        Record s = Db.findFirst(sql);
        Db.update("update soul set hits=hits+1 where id=?", s.getInt("id"));

        data.put(ResponseCode.LIST, s);
        rest.success().setData(data);
        renderJson(rest);
    }

    @Clear(WebLoginInterceptor.class)
    public void bullshit(){
        render("main/bullshit.html");
    }

    @Clear(WebLoginInterceptor.class)
    public void genBullshit(){
        String topic = getPara("topic");
        BullshitGenerator bs = new BullshitGenerator();
        bs.initData(topic);
        String article = bs.genArticle();
        //将内容写入缓存，用于后续的生成静态分享页
        String key = HashKit.md5(DateUtils.getLongDateMilliSecond()+"");
        data.put("bKey", key);
        data.put("bData", article);
        JSONObject jo = new JSONObject();
        jo.put("topic", topic);
        jo.put("article", article);
        CacheKit.put("BullshitCache", key, jo);
        rest.success().setData(data);
        renderJson(rest);
    }

    @Clear(WebLoginInterceptor.class)
    public void genSharePage(){
        boolean r = false;
        String bKey = getPara("bKey");
        JSONObject jo = CacheKit.get("BullshitCache", bKey);
        if(null != jo){
            //静态页地址
            String pageName = DateUtils.getLongDateMilliSecond()+".html";
            String pagePath = PathKit.getWebRootPath()+"/page/"+pageName;
            PageGenerator pg = new PageGenerator();
            HashMap<String, Object> param = new HashMap<String, Object>();
            String topic = jo.getString("topic");
            param.put("topic", topic);
            param.put("article", jo.get("article"));
            //拆成拼音与汉字
            char topicList[] = topic.toCharArray();
            HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
            format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
            format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
            JSONArray phArray = new JSONArray();
            try{
                for(char c : topicList){
                    String[] rs = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    JSONObject ph = new JSONObject();
                    ph.put("py", rs[0]);
                    ph.put("han", c);
                    phArray.add(ph);
                }
                param.put("phList", phArray);
                r = pg.generate( pagePath, param);
                if(r){
                    rest.success("分享页生成成功！").setData(pageName);
                }else{
                    rest.error("分享页生成失败！");
                }
            }catch (BadHanyuPinyinOutputFormatCombination ex){
                ex.printStackTrace();
                rest.error("废话机出错!");
            }
        }else{
            rest.error("废话机出错！");
        }
        renderJson(rest);
    }
}

package com.sagacity.docs.wxss.controller.v1;

import com.jfinal.aop.Before;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.model.doc.DocInfo;
import com.sagacity.docs.base.extend.ResponseCode;
import com.sagacity.docs.model.question.Question;
import com.sagacity.docs.model.question.QuestionReply;
import com.sagacity.docs.wxss.common.WXSSBaseController;
import com.sagacity.utility.DateUtils;

/**
 * Created by mulaliu on 16/4/15.
 */
@ControllerBind(controllerKey = "/wxss/question", viewPath = "/wxss")
public class QuestionController extends WXSSBaseController {


    @Override
    public void index() {
        int page = getParaToInt("page"); //数据分页

        String qs_select = "select qs.id,qs.title,qs.desc,qs.created_at,qs.source_id,qs.source,user.nick_name,ul.title level_title,user.gender,user.avatar_url,ifNULL(qrc.reply_count,0) reply_count\n";
        String qs_from = "from question qs\n" +
                "left join (select count(id) reply_count,question_id from question_reply group by question_id) qrc on qrc.question_id=qs.id\n"+
                "left join wx_user user on user.open_id=qs.open_id\n" +
                "left join wx_user_level ul on ul.id=user.level_id\n" +
                "where qs.state=1\n" +
                "order by qs.created_at DESC";
        Page<Record> questionList = Db.paginate(page,  getParaToInt("pageSize", 12),
                qs_select, qs_from);
        //添加图片附件和来源
        for (Record question : questionList.getList()){
            question.set("source_info", DocInfo.dao.getSource(question.getStr("source"), question.getInt("source_id")));
        }
        renderJson(questionList);
    }

    @Before(Tx.class)
    public void questionPost(){
        boolean r = false;
        String token = getPara("token");
        String title = getPara("title");
        String desc = getPara("desc");

        //处理主数据
        r = new Question().set("parent_id", getPara("parent_id"))
                .set("res_id", getPara("res_id")).set("open_id", token).set("title", title).set("desc", desc)
                .set("source_id", getPara("source_id")).set("source", getPara("source")).set("view_count",0)
                .set("created_at", DateUtils.nowDateTime()).set("state", 1).save();
        //处理图片附件

        if(r){
            responseData.put(ResponseCode.MSG, "提问成功！");
        }else{
            responseData.put(ResponseCode.MSG, "提问失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    public void getQuestionInfo(){
        int page = getParaToInt("page"); //数据分页
        int id = getParaToInt("id", 0);
        //获得问题相关信息
        String qsSql = "select qs.id,qs.title,qs.desc,qs.view_count,qs.created_at,u.nick_name,u.avatar_url,ul.title level_title,qs.source,qs.source_id\n" +
                ",IFNULL(ulc.like_count,0) like_count\n" +
                "from question qs\n" +
                "left join (select count(id) like_count,data_id from user_like ul where type='qs' group by data_id ) ulc on ulc.data_id=qs.id\n" +
                "left join wx_user u on u.open_id=qs.open_id\n" +
                "left join wx_user_level ul on ul.id=u.level_id\n" +
                "where qs.id=?";
        Record qs = Db.findFirst(qsSql, id);
        qs.set("source_info", DocInfo.dao.getSource(qs.getStr("source"), qs.getInt("source_id")));
        //添加图片附件
        responseData.put("qs", qs);
        //回答数据
        String qsr_select = "select qr.id,qr.content,qr.view_count,qr.created_at,u.nick_name,u.avatar_url,ul.title level_title\n" +
                ",IFNULL(ulc.like_count,0) like_count,qr.is_accept\n";
        String qsr_from = "from question_reply qr\n" +
                "left join (select count(id) like_count,data_id from user_like ul where type='qsr' group by data_id ) ulc on ulc.data_id=qr.id\n" +
                "left join wx_user u on u.open_id=qr.open_id\n" +
                "left join wx_user_level ul on ul.id=u.level_id\n" +
                "where qr.state=1 and qr.question_id=?\n" +
                "order by qr.created_at DESC";
        Page<Record> replyList = Db.paginate(page,  getParaToInt("pageSize", 12),
                qsr_select, qsr_from, id);
        responseData.put("replys", replyList);
        renderJson(responseData);
    }

    @Before(Tx.class)
    public void replyPost(){
        boolean r = false;
        String token = getPara("token");
        String id = getPara("id");
        String content = getPara("content");

        //处理主数据
        r = new QuestionReply().set("question_id", id)
                .set("open_id", token).set("content", content)
                .set("view_count",0).set("is_accept", 0)
                .set("created_at", DateUtils.nowDateTime()).set("state", 1).save();
        //处理图片附件

        if(r){
            responseData.put(ResponseCode.MSG, "回答成功！");
        }else{
            responseData.put(ResponseCode.MSG, "回答失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

}

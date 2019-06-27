package com.sagacity.docs.wxss.comment;

import com.jfinal.aop.Before;
import com.jfinal.ext.route.ControllerBind;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.sagacity.docs.comment.Comment;
import com.sagacity.docs.extend.ResponseCode;
import com.sagacity.docs.wxss.WXSSBaseController;
import com.sagacity.utility.DateUtils;

@ControllerBind(controllerKey = "/wxss/comment", viewPath = "/wxss")
public class CommentController extends WXSSBaseController{

    @Override
    public void index(){

    }

    /**
     * 获取评论列表
     */
    public void getCommentList(){
        int page = getParaToInt("page"); //数据分页

        int data_id = getParaToInt("data_id");
        String type = getPara("type");

        String sqlSelect = "select ac.id,p1.open_id,p1.nick_name,p1.avatar_url,ac.refer_id,p2.open_id ref_open_id,p2.nick_name ref_nick_name\n" +
                ",ac.content,ac.created_at";
        String sqlFrom ="from comment ac\n" +
                "left join wx_user p1 on p1.open_id=ac.open_id\n" +
                "left join comment acr on acr.id=ac.refer_id\n" +
                "left join wx_user p2 on p2.open_id=acr.open_id\n" +
                "where ac.state=1 and ac.source_id=? and ac.source=?";
        sqlFrom +=" order by ac.created_at DESC";

        renderJson(Db.paginate(page, getParaToInt("pageSize",10), sqlSelect, sqlFrom
                , data_id, type));
    }

    @Before(Tx.class)
    public void addComment(){
        int data_id = getParaToInt("data_id");
        int refer_id = getParaToInt("refer_id", 0);
        String type = getPara("type");

        String token = getPara("token");
        String content = getPara("content");
        boolean r = false;

        Comment ac = new Comment().set("refer_id",refer_id).set("source_id", data_id).set("source", type)
                .set("open_id", token).set("form_id", getPara("form_id"))
                .set("title", getPara("title", "")).set("content", content)
                .set("state", 1).set("created_at", DateUtils.nowDateTime());
        r = ac.save();
        //推送模板消息
        if(refer_id >0){
//            WXMessage.dao.sendCommentReply(Module.Activity, refer_id, ac.getInt("CommentID"));
        }
        if(r){
            responseData.put(ResponseCode.MSG, "留言成功！");
        }else{
            responseData.put(ResponseCode.MSG, "留言失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

    /**
     * 删除留言
     */
    @Before(Tx.class)
    public void delComment(){
        String token = getPara("token");
        int data_id = getParaToInt("data_id");
        boolean r = false;

//        r = Comment.dao.deleteById(commentID);
        r = Comment.dao.findById(data_id).set("state", 0).update();
        if(r){
            responseData.put(ResponseCode.MSG, "删除成功！");
        }else{
            responseData.put(ResponseCode.MSG, "删除失败！");
        }
        responseData.put(ResponseCode.CODE, r? 1:0);
        renderJson(responseData);
    }

}
